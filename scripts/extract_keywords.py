import re
import json
import os
import subprocess
import sys

# List of keywords defined in Chapter Six of core PDF
KEYWORDS = [
    "Aggravated", "Bridge", "Clash", "Counterattack", "Decisive-only",
    "Dual", "Form", "Mastery", "Mute", "Perilous", "Pilot", 
    "Psyche", "Salient", "Stackable", "Terrestrial", "Uniform", 
    "Withering-only", "Written-only"
]

# Set output directory relative to project root
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
OUTPUT_FILE = os.path.join(PROJECT_ROOT, "src", "main", "resources", "charms", "keywords.json")

def clean_text(text):
    # Remove PDF artifacts
    text = re.sub(r'\n\d+\nEX3\n', '\n', text)
    text = re.sub(r'\nEX3\n', '\n', text)
    text = re.sub(r'\nCHARMS\n', '\n', text)
    text = re.sub(r'\nCHAPTER \d+\n', '\n', text)
    
    lines = text.strip().split('\n')
    cleaned_lines = []
    current_para = ""
    
    for line in lines:
        line = line.strip()
        if not line:
            if current_para:
                cleaned_lines.append(current_para)
                current_para = ""
            continue
            
        if current_para:
            prev_content = current_para.strip()
            prev_char = prev_content[-1] if prev_content else ""
            
            # Use same smart line join logic as extract_charms.py
            is_bullet = line.startswith('•') or line.startswith('- ') or line.startswith('* ')
            is_continuation = line[0].islower() or prev_char == ','
            
            if is_bullet or (not is_continuation and prev_char in ('.', '!', '?', ':', ';')):
                cleaned_lines.append(current_para)
                current_para = line
            else:
                current_para += " " + line
        else:
            current_para = line
            
    if current_para:
        cleaned_lines.append(current_para)
        
    return "\n\n".join(cleaned_lines)

def extract_keywords(pdf_path):
    # Keyword section is usually pages 252-254 (1-indexed) in Core
    print(f"Dumping keyword pages (252-255) from {pdf_path}...")
    # pdftotext -f [first] -l [last]
    cmd = ["pdftotext", "-f", "252", "-l", "255", pdf_path, "-"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("Error running pdftotext")
        return

    text = result.stdout
    
    output_keywords = []
    
    # We look for keyword blocks. Each keyword usually starts with the name, 
    # followed by the description.
    # Regex: Find the keyword names as anchors.
    # Note: Keyword names are usually at the start of a block.
    
    # Creating a mega-regex for all keywords
    # Pattern: \n[Keyword]\n
    keyword_pattern = r'\n(' + '|'.join(KEYWORDS) + r')\n'
    
    segments = re.split(keyword_pattern, text)
    # segments[0] is preamble
    # segments[1] is keyword[0] name
    # segments[2] is keyword[0] description
    
    for i in range(1, len(segments), 2):
        name = segments[i]
        raw_desc = segments[i+1] if i+1 < len(segments) else ""
        
        # Trim description until the next keyword (the split already did most of this)
        # However, we might have noise or the text might extend into the next section.
        # We'll take the text until the first major header or end of segment.
        desc = clean_text(raw_desc)
        
        output_keywords.append({
            "name": name,
            "description": desc
        })
        print(f"Extracted Keyword: {name}")

    # Save to file
    out_dir = os.path.dirname(OUTPUT_FILE)
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)
        
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(output_keywords, f, indent=4, ensure_ascii=False)
    
    print(f"Saved {len(output_keywords)} keywords to {OUTPUT_FILE}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 extract_keywords.py <path_to_core_pdf>")
        sys.exit(1)
        
    pdf_path = sys.argv[1]
    if not os.path.exists(pdf_path):
        print(f"Error: File not found: {pdf_path}")
        sys.exit(1)
        
    extract_keywords(pdf_path)
