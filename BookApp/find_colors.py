import os
import re

layout_dir = r"d:\LT-APP\Book_App\BTL-Mobile\BookApp\app\src\main\res\layout"

def find_text_colors():
    colors = set()
    for filename in os.listdir(layout_dir):
        if filename.endswith(".xml"):
            filepath = os.path.join(layout_dir, filename)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            matches = re.findall(r'android:textColor="([^"]+)"', content)
            colors.update(matches)
    
    for color in sorted(colors):
        print(color)

if __name__ == "__main__":
    find_text_colors()
