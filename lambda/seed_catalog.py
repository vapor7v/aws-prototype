"""
Seed script for DynamoDB FurnitureCatalog table.
Run: python seed_catalog.py

Requires AWS credentials configured (aws configure) and the table already created.
"""

import boto3
import json

dynamodb = boto3.resource('dynamodb', region_name='us-east-1')
table = dynamodb.Table('FurnitureCatalog')

CATALOG_ITEMS = [
    # ─── Seating ────────────────────────────────────────────
    {
        'category': 'seating', 'id': 'sofa_minimal_01',
        'name': 'Minimalist 3-Seater Sofa',
        'style_tags': ['minimalist', 'modern', 'scandinavian'],
        'dimensions': {'width': 2.1, 'height': 0.85, 'depth': 0.9},
        'model_s3_key': 'furniture/sofa_minimal_01.glb',
        'thumbnail_key': 'thumbnails/sofa_minimal_01.webp',
        'color_hex': '#E8DDD0', 'material': 'fabric',
        'placement': 'FLOOR', 'price_inr': 18000
    },
    {
        'category': 'seating', 'id': 'sofa_modern_01',
        'name': 'Modern L-Shape Sofa',
        'style_tags': ['modern', 'industrial'],
        'dimensions': {'width': 2.6, 'height': 0.80, 'depth': 1.6},
        'model_s3_key': 'furniture/sofa_modern_01.glb',
        'thumbnail_key': 'thumbnails/sofa_modern_01.webp',
        'color_hex': '#4A4A4A', 'material': 'leather',
        'placement': 'FLOOR', 'price_inr': 35000
    },
    {
        'category': 'seating', 'id': 'chair_accent_01',
        'name': 'Accent Armchair',
        'style_tags': ['modern', 'cozy', 'bohemian'],
        'dimensions': {'width': 0.75, 'height': 0.85, 'depth': 0.80},
        'model_s3_key': 'furniture/chair_accent_01.glb',
        'thumbnail_key': 'thumbnails/chair_accent_01.webp',
        'color_hex': '#D4A574', 'material': 'fabric',
        'placement': 'FLOOR', 'price_inr': 8500
    },
    {
        'category': 'seating', 'id': 'chair_dining_01',
        'name': 'Wooden Dining Chair',
        'style_tags': ['scandinavian', 'minimalist', 'traditional'],
        'dimensions': {'width': 0.45, 'height': 0.90, 'depth': 0.50},
        'model_s3_key': 'furniture/chair_dining_01.glb',
        'thumbnail_key': 'thumbnails/chair_dining_01.webp',
        'color_hex': '#8B6914', 'material': 'wood',
        'placement': 'FLOOR', 'price_inr': 3200
    },

    # ─── Tables ─────────────────────────────────────────────
    {
        'category': 'table', 'id': 'table_coffee_01',
        'name': 'Round Coffee Table',
        'style_tags': ['minimalist', 'modern', 'scandinavian'],
        'dimensions': {'width': 0.80, 'height': 0.45, 'depth': 0.80},
        'model_s3_key': 'furniture/table_coffee_01.glb',
        'thumbnail_key': 'thumbnails/table_coffee_01.webp',
        'color_hex': '#D4A574', 'material': 'wood',
        'placement': 'FLOOR', 'price_inr': 6500
    },
    {
        'category': 'table', 'id': 'table_side_01',
        'name': 'Mid-Century Side Table',
        'style_tags': ['modern', 'minimalist'],
        'dimensions': {'width': 0.45, 'height': 0.55, 'depth': 0.45},
        'model_s3_key': 'furniture/table_side_01.glb',
        'thumbnail_key': 'thumbnails/table_side_01.webp',
        'color_hex': '#C19A6B', 'material': 'wood',
        'placement': 'FLOOR', 'price_inr': 3800
    },
    {
        'category': 'table', 'id': 'table_dining_01',
        'name': 'Rustic Dining Table',
        'style_tags': ['industrial', 'traditional', 'cozy'],
        'dimensions': {'width': 1.6, 'height': 0.76, 'depth': 0.90},
        'model_s3_key': 'furniture/table_dining_01.glb',
        'thumbnail_key': 'thumbnails/table_dining_01.webp',
        'color_hex': '#6B4226', 'material': 'wood',
        'placement': 'FLOOR', 'price_inr': 22000
    },
    {
        'category': 'table', 'id': 'shelf_book_01',
        'name': 'Open Bookshelf',
        'style_tags': ['modern', 'industrial', 'scandinavian'],
        'dimensions': {'width': 0.80, 'height': 1.80, 'depth': 0.35},
        'model_s3_key': 'furniture/shelf_book_01.glb',
        'thumbnail_key': 'thumbnails/shelf_book_01.webp',
        'color_hex': '#2C2C2C', 'material': 'metal',
        'placement': 'FLOOR', 'price_inr': 9500
    },

    # ─── Rugs ───────────────────────────────────────────────
    {
        'category': 'rug', 'id': 'rug_geometric_01',
        'name': 'Geometric Area Rug',
        'style_tags': ['modern', 'scandinavian', 'minimalist'],
        'dimensions': {'width': 2.0, 'height': 0.02, 'depth': 1.5},
        'model_s3_key': 'furniture/rug_geometric_01.glb',
        'thumbnail_key': 'thumbnails/rug_geometric_01.webp',
        'color_hex': '#C8B89A', 'material': 'wool',
        'placement': 'FLOOR', 'price_inr': 7200
    },
    {
        'category': 'rug', 'id': 'rug_shag_01',
        'name': 'Plush Shag Rug',
        'style_tags': ['cozy', 'bohemian'],
        'dimensions': {'width': 1.8, 'height': 0.05, 'depth': 1.2},
        'model_s3_key': 'furniture/rug_shag_01.glb',
        'thumbnail_key': 'thumbnails/rug_shag_01.webp',
        'color_hex': '#F5F0EB', 'material': 'polyester',
        'placement': 'FLOOR', 'price_inr': 5500
    },

    # ─── Wall Art ───────────────────────────────────────────
    {
        'category': 'wall_art', 'id': 'painting_abstract_01',
        'name': 'Abstract Canvas Print',
        'style_tags': ['modern', 'minimalist'],
        'dimensions': {'width': 0.80, 'height': 0.60, 'depth': 0.03},
        'model_s3_key': 'wall_art/painting_abstract_01.glb',
        'thumbnail_key': 'thumbnails/painting_abstract_01.webp',
        'color_hex': '#2C5F8A', 'material': 'canvas',
        'placement': 'WALL', 'price_inr': 2800,
        'default_wall_height': 1.5
    },
    {
        'category': 'wall_art', 'id': 'painting_abstract_02',
        'name': 'Earth Tones Triptych',
        'style_tags': ['bohemian', 'cozy', 'traditional'],
        'dimensions': {'width': 1.20, 'height': 0.50, 'depth': 0.03},
        'model_s3_key': 'wall_art/painting_abstract_02.glb',
        'thumbnail_key': 'thumbnails/painting_abstract_02.webp',
        'color_hex': '#A0785A', 'material': 'canvas',
        'placement': 'WALL', 'price_inr': 4200,
        'default_wall_height': 1.5
    },
    {
        'category': 'wall_art', 'id': 'mirror_round_01',
        'name': 'Round Brass Mirror',
        'style_tags': ['modern', 'scandinavian', 'minimalist'],
        'dimensions': {'width': 0.60, 'height': 0.60, 'depth': 0.05},
        'model_s3_key': 'wall_art/mirror_round_01.glb',
        'thumbnail_key': 'thumbnails/mirror_round_01.webp',
        'color_hex': '#D4AF37', 'material': 'metal',
        'placement': 'WALL', 'price_inr': 3500,
        'default_wall_height': 1.5
    },
    {
        'category': 'wall_art', 'id': 'clock_wall_01',
        'name': 'Industrial Wall Clock',
        'style_tags': ['industrial', 'modern'],
        'dimensions': {'width': 0.40, 'height': 0.40, 'depth': 0.05},
        'model_s3_key': 'wall_art/clock_wall_01.glb',
        'thumbnail_key': 'thumbnails/clock_wall_01.webp',
        'color_hex': '#1A1A1A', 'material': 'metal',
        'placement': 'WALL', 'price_inr': 1800,
        'default_wall_height': 1.6
    },

    # ─── Wall Lights ────────────────────────────────────────
    {
        'category': 'wall_light', 'id': 'sconce_minimal_01',
        'name': 'Minimalist Wall Sconce',
        'style_tags': ['minimalist', 'modern', 'scandinavian'],
        'dimensions': {'width': 0.12, 'height': 0.25, 'depth': 0.15},
        'model_s3_key': 'lighting/sconce_minimal_01.glb',
        'thumbnail_key': 'thumbnails/sconce_minimal_01.webp',
        'color_hex': '#D4AF37', 'material': 'brass',
        'placement': 'WALL', 'price_inr': 2200,
        'emissive': True, 'default_wall_height': 1.8
    },
    {
        'category': 'wall_light', 'id': 'sconce_industrial_01',
        'name': 'Industrial Cage Sconce',
        'style_tags': ['industrial', 'modern'],
        'dimensions': {'width': 0.15, 'height': 0.30, 'depth': 0.20},
        'model_s3_key': 'lighting/sconce_industrial_01.glb',
        'thumbnail_key': 'thumbnails/sconce_industrial_01.webp',
        'color_hex': '#2C2C2C', 'material': 'iron',
        'placement': 'WALL', 'price_inr': 1800,
        'emissive': True, 'default_wall_height': 1.8
    },
    {
        'category': 'wall_light', 'id': 'lamp_floor_01',
        'name': 'Arc Floor Lamp',
        'style_tags': ['modern', 'minimalist'],
        'dimensions': {'width': 0.40, 'height': 1.80, 'depth': 0.40},
        'model_s3_key': 'lighting/lamp_floor_01.glb',
        'thumbnail_key': 'thumbnails/lamp_floor_01.webp',
        'color_hex': '#D4AF37', 'material': 'brass',
        'placement': 'FLOOR', 'price_inr': 6800,
        'emissive': True
    },
    {
        'category': 'wall_light', 'id': 'lamp_table_01',
        'name': 'Ceramic Table Lamp',
        'style_tags': ['cozy', 'traditional', 'bohemian'],
        'dimensions': {'width': 0.25, 'height': 0.45, 'depth': 0.25},
        'model_s3_key': 'lighting/lamp_table_01.glb',
        'thumbnail_key': 'thumbnails/lamp_table_01.webp',
        'color_hex': '#F5F0EB', 'material': 'ceramic',
        'placement': 'FLOOR', 'price_inr': 2400,
        'emissive': True
    },
]


def seed():
    print(f'Seeding {len(CATALOG_ITEMS)} items into FurnitureCatalog...')

    with table.batch_writer() as batch:
        for item in CATALOG_ITEMS:
            # Convert numeric values to strings for DynamoDB compatibility
            db_item = {}
            for key, value in item.items():
                if isinstance(value, float):
                    db_item[key] = str(value)
                elif isinstance(value, dict):
                    db_item[key] = {k: str(v) if isinstance(v, float) else v for k, v in value.items()}
                else:
                    db_item[key] = value
            batch.put_item(Item=db_item)
            print(f'  ✓ {item["category"]}/{item["id"]} — {item["name"]}')

    print(f'\nDone! {len(CATALOG_ITEMS)} items seeded.')


if __name__ == '__main__':
    seed()
