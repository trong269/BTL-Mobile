import os
import re

drawable_dir = r"d:\LT-APP\Book_App\BTL-Mobile\BookApp\app\src\main\res\drawable"

def extract():
    colors = set()
    for filename in os.listdir(drawable_dir):
        if filename.endswith(".xml"):
            filepath = os.path.join(drawable_dir, filename)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            matches = re.findall(r'android:color="([^"]+)"', content)
            colors.update([m for m in matches if m.startswith('#')])
    
    print("\n".join(sorted(colors)))

if __name__ == "__main__":
    extract()
