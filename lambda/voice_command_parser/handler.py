import json
import boto3
import os

bedrock = boto3.client('bedrock-runtime', region_name=os.environ.get('AWS_REGION', 'us-east-1'))

SYSTEM_PROMPT = """You are a voice command parser for an AR interior design app. Given a transcribed voice command, extract a structured action as JSON.

Return ONLY valid JSON with this exact structure:
{
  "target": "sofa|table|rug|chair|lamp|wall|painting|sconce|all",
  "action": "MOVE|RECOLOR|REMOVE|RESIZE|ROTATE|UNDO|RESET",
  "direction": "LEFT|RIGHT|FORWARD|BACKWARD|UP|DOWN" or null,
  "amount": 0.3,
  "color": "#HEXCOLOR" or null,
  "scale_factor": 1.2 or null,
  "rotation_degrees": 45 or null,
  "confidence": 0.95
}

Rules:
- "amount" is in meters for movement (default 0.3m for small moves, 0.5m for medium, 1.0m for large)
- "scale_factor" is relative (1.2 = 20% bigger, 0.8 = 20% smaller)
- For "UNDO", set target to "last_action" and all other fields to null
- For "RESET", set target to "all" and all other fields to null
- For "RECOLOR", the target should be "wall" and color must be provided
- confidence is your confidence in parsing (0.0-1.0)
- If command is unclear, set confidence < 0.5
"""

MODEL_ID = os.environ.get('BEDROCK_MODEL_ID', 'anthropic.claude-3-haiku-20240307-v1:0')


def lambda_handler(event, context):
    try:
        body = json.loads(event.get('body', '{}'))
        text = body.get('text', '')

        if not text:
            return {
                'statusCode': 400,
                'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
                'body': json.dumps({'error': 'Missing "text" field'})
            }

        response = bedrock.invoke_model(
            modelId=MODEL_ID,
            body=json.dumps({
                'anthropic_version': 'bedrock-2023-05-31',
                'max_tokens': 300,
                'system': SYSTEM_PROMPT,
                'messages': [
                    {'role': 'user', 'content': text}
                ]
            })
        )

        response_body = json.loads(response['body'].read())
        assistant_text = response_body['content'][0]['text']
        voice_command = json.loads(assistant_text)

        return {
            'statusCode': 200,
            'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({
                'command': voice_command,
                'raw_text': text
            })
        }

    except json.JSONDecodeError as e:
        return {
            'statusCode': 422,
            'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({'error': f'Failed to parse command: {str(e)}'})
        }
    except Exception as e:
        return {
            'statusCode': 500,
            'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({'error': str(e)})
        }
