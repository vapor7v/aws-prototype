import json
import boto3
import os

bedrock = boto3.client('bedrock-runtime', region_name=os.environ.get('AWS_REGION', 'us-east-1'))

SYSTEM_PROMPT = """You are an interior design AI assistant. Given a user's room description, extract a structured design intent as JSON.

Return ONLY valid JSON with this exact structure:
{
  "style": "minimalist|modern|cozy|industrial|scandinavian|bohemian|traditional",
  "palette": {
    "primary": "#HEXCOLOR",
    "secondary": "#HEXCOLOR",
    "accent": "#HEXCOLOR"
  },
  "objects": [
    {"type": "sofa|table|rug|chair|shelf|lamp|bed", "style": "string", "material": "string", "color": "string"}
  ],
  "wall_treatment": {
    "color": "#HEXCOLOR",
    "texture": "matte|brick|wood|wallpaper",
    "accent_wall": {"wall_index": 0, "color": "#HEXCOLOR"} or null
  },
  "wall_decor": [
    {"type": "painting|frame|mirror|shelf|clock", "style": "string", "size": "small|medium|large"}
  ],
  "accent_lighting": [
    {"type": "wall_sconce|led_strip|spotlight|floor_lamp", "position": "string", "warmth": "warm|neutral|cool", "color": "#HEXCOLOR"}
  ],
  "constraints": {
    "budget_max": number or null,
    "currency": "string" or null
  }
}

Rules:
- Always return valid JSON, nothing else
- If user doesn't mention a category, provide sensible defaults based on the style
- Generate 3-5 furniture objects minimum
- Include at least 1 wall decor and 1 accent lighting item
- Color palette should be harmonious and match the requested style
"""

MODEL_ID = os.environ.get('BEDROCK_MODEL_ID', 'anthropic.claude-3-haiku-20240307-v1:0')


def lambda_handler(event, context):
    try:
        body = json.loads(event.get('body', '{}'))
        user_prompt = body.get('prompt', '')

        if not user_prompt:
            return {
                'statusCode': 400,
                'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
                'body': json.dumps({'error': 'Missing "prompt" field'})
            }

        # Call Bedrock Claude Haiku
        response = bedrock.invoke_model(
            modelId=MODEL_ID,
            body=json.dumps({
                'anthropic_version': 'bedrock-2023-05-31',
                'max_tokens': 1500,
                'system': SYSTEM_PROMPT,
                'messages': [
                    {'role': 'user', 'content': user_prompt}
                ]
            })
        )

        response_body = json.loads(response['body'].read())
        assistant_text = response_body['content'][0]['text']

        # Parse the JSON from Claude's response
        design_intent = json.loads(assistant_text)

        return {
            'statusCode': 200,
            'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({
                'design_intent': design_intent,
                'raw_prompt': user_prompt
            })
        }

    except json.JSONDecodeError as e:
        return {
            'statusCode': 422,
            'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({'error': f'Failed to parse AI response as JSON: {str(e)}', 'raw_response': assistant_text if 'assistant_text' in dir() else None})
        }
    except Exception as e:
        return {
            'statusCode': 500,
            'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({'error': str(e)})
        }
