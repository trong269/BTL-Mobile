import os
import re

layout_dir = r"d:\LT-APP\Book_App\BTL-Mobile\BookApp\app\src\main\res\layout"

def get_replacement(hex_color):
    hex_color = hex_color.upper()
    
    # Exceptions
    if hex_color in ['#B43A2D', '#F9A825', '#FFFFFF'] or hex_color.startswith('#FFF'):
        return None
        
    # Heuristic based on the first hex digit after #
    if len(hex_color) >= 7:
        first_digit = hex_color[1]
        if first_digit in '01234':
            return '@color/text_primary'
        elif first_digit in '567':
            return '@color/text_secondary'
        elif first_digit in '89ABCDEF':
            return '@color/text_muted'
            
    return '@color/text_primary' # fallback

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content
    
    # Find all hex colors in android:textColor
    matches = set(re.findall(r'android:textColor="([^"]+)"', content))
    
    for hex_color in matches:
        if hex_color.startswith('#'):
            replacement = get_replacement(hex_color)
            if replacement:
                # Replace specifically in textColor context to avoid replacing backgrounds
                pattern = re.compile(f'android:textColor="{hex_color}"', re.IGNORECASE)
                content = pattern.sub(f'android:textColor="{replacement}"', content)

    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {os.path.basename(filepath)}")

def main():
    if not os.path.exists(layout_dir):
        print(f"Directory not found: {layout_dir}")
        return

    for filename in os.listdir(layout_dir):
        if filename.endswith(".xml"):
            filepath = os.path.join(layout_dir, filename)
            process_file(filepath)

if __name__ == "__main__":
    main()
