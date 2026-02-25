import json
import boto3
import base64
import os

rekognition = boto3.client('rekognition', region_name=os.environ.get('AWS_REGION', 'us-east-1'))
dynamodb = boto3.resource('dynamodb', region_name=os.environ.get('AWS_REGION', 'us-east-1'))

TABLE_NAME = os.environ.get('CATALOG_TABLE', 'FurnitureCatalog')
CLOUDFRONT_DOMAIN = os.environ.get('CLOUDFRONT_DOMAIN', '')

# Mapping from Rekognition labels to catalog categories
LABEL_TO_CATEGORY = {
    'Chair': 'seating',
    'Armchair': 'seating',
    'Sofa': 'seating',
    'Couch': 'seating',
    'Table': 'table',
    'Coffee Table': 'table',
    'Desk': 'table',
    'Dining Table': 'table',
    'Rug': 'rug',
    'Carpet': 'rug',
    'Lamp': 'wall_light',
    'Chandelier': 'wall_light',
    'Light Fixture': 'wall_light',
    'Painting': 'wall_art',
    'Art': 'wall_art',
    'Frame': 'wall_art',
    'Mirror': 'wall_art',
    'Shelf': 'table',
    'Bookcase': 'table',
    'Bed': 'seating',
    'Cabinet': 'table',
}


def lambda_handler(event, context):
    try:
        body = json.loads(event.get('body', '{}'))
        image_base64 = body.get('image', '')

        if not image_base64:
            return {
                'statusCode': 400,
                'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
                'body': json.dumps({'error': 'Missing "image" field (base64)'})
            }

        # Decode base64 image
        image_bytes = base64.b64decode(image_base64)

        # Call Rekognition
        rekog_response = rekognition.detect_labels(
            Image={'Bytes': image_bytes},
            MaxLabels=15,
            MinConfidence=70
        )

        labels = [
            {'name': label['Name'], 'confidence': label['Confidence']}
            for label in rekog_response['Labels']
        ]

        # Find best matching catalog category
        catalog_match = None
        matched_category = None
        matched_label = None

        for label in labels:
            category = LABEL_TO_CATEGORY.get(label['name'])
            if category:
                matched_category = category
                matched_label = label['name']
                break

        # Query DynamoDB for matching catalog items
        if matched_category:
            table = dynamodb.Table(TABLE_NAME)
            response = table.query(
                KeyConditionExpression=boto3.dynamodb.conditions.Key('category').eq(matched_category),
                Limit=5
            )

            items = response.get('Items', [])
            if items:
                # Pick first match (could be enhanced with style matching)
                catalog_match = items[0]

        # Build response
        result = {
            'labels': labels,
            'matched_label': matched_label,
            'matched_category': matched_category,
        }

        if catalog_match:
            model_key = catalog_match.get('model_s3_key', '')
            result['catalog_match'] = {
                'id': catalog_match.get('id', ''),
                'name': catalog_match.get('name', ''),
                'category': catalog_match.get('category', ''),
                'style_tags': catalog_match.get('style_tags', []),
                'dimensions': catalog_match.get('dimensions', {}),
                'color_hex': catalog_match.get('color_hex', ''),
            }
            if CLOUDFRONT_DOMAIN:
                result['model_url'] = f'https://{CLOUDFRONT_DOMAIN}/{model_key}'
            else:
                result['model_url'] = f's3://{os.environ.get("MODEL_BUCKET", "ar-interior-models")}/{model_key}'
        else:
            result['catalog_match'] = None
            result['model_url'] = None

        return {
            'statusCode': 200,
            'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
            'body': json.dumps(result)
        }

    except Exception as e:
        return {
            'statusCode': 500,
            'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({'error': str(e)})
        }
