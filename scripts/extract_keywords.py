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
    # We broaden to 250-258 to capture context and all keywords
    print(f"Dumping keyword pages (250-260) from {pdf_path}...")
    cmd = ["pdftotext", "-f", "250", "-l", "260", pdf_path, "-"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("Error running pdftotext")
        return

    text = result.stdout
    
    output_keywords = []
    
    # regex matches bullet point, whitespace, word/keyword, and then colon (example: •  Mute: description)
    # Using re.split with a capturing group to keep the delimiters (keyword names)
    keyword_block_pattern = r'•\s+([A-Z][A-Za-z0-9-]+):'
    
    segments = re.split(keyword_block_pattern, text)
    # segments[0] is preamble before any bullet
    # segments[1] is 1st keyword name
    # segments[2] is 1st keyword description
    # ...
    
    for i in range(1, len(segments), 2):
        name = segments[i].strip()
        raw_desc = segments[i+1].strip() if i+1 < len(segments) else ""
        
        # Descriptions end where the next section or footer starts.
        # Often marked by text like "Duration:", "Prerequisite Charms:", or page numbers.
        # We'll take everything until the next major header.
        stop_words = ["Duration:", "Prerequisite Charms:", "EX3", "CHAPTER 6"]
        for stop in stop_words:
            if stop in raw_desc:
                raw_desc = raw_desc.split(stop)[0].strip()
        
        desc = clean_text(raw_desc)
        
        # Only add if it's actually in our known list of keywords found in charms
        # This prevents picking up false positives from other bulleted lists
        if name in KEYWORDS:
            output_keywords.append({
                "name": name,
                "description": desc
            })
            print(f"Extracted Keyword: {name}")

    # Manual Fallback for keywords that might be in different formats (Terrestrial, Mastery)
    # In some book versions, they are in a sidebar box.
    if "Mastery" not in [k["name"] for k in output_keywords] and "Mastery" in KEYWORDS:
        # Simple extraction for Mastery box if found
        mastery_match = re.search(r'MASTER’S HAND: SOLAR MASTERY AND TERRESTRIAL EFFECTS(.*?)(?=CHAPTER|EX3|\d{3})', text, re.DOTALL)
        if mastery_match:
            desc = clean_text(mastery_match.group(1))
            output_keywords.append({"name": "Mastery", "description": desc})
            output_keywords.append({"name": "Terrestrial", "description": desc}) # Often they sharing description block
            print("Extracted Mastery/Terrestrial from sidebar box.")

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
