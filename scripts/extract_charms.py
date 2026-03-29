import re
import json
import os
import subprocess
import sys

# List of official abilities to match in the "Mins:" line
ABILITIES = [
    "Archery", "Athletics", "Awareness", "Brawl", "Bureaucracy",
    "Craft", "Dodge", "Integrity", "Investigation", "Larceny",
    "Linguistics", "Lore", "Martial Arts", "Medicine", "Melee",
    "Occult", "Performance", "Presence", "Resistance", "Ride",
    "Sail", "Socialize", "Stealth", "Survival", "Thrown", "War"
]

# Set output directory relative to project root (one level up from this script)
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "src", "main", "resources", "charms")

def clean_description(text):
    # Remove page numbers, chapter headers, etc.
    # Ex: "262\nEX3", "CHARMS", "CHAPTER 6"
    text = re.sub(r'\n\d+\nEX3\n', '\n', text)
    text = re.sub(r'\nEX3\n', '\n', text)
    text = re.sub(r'\nCHARMS\n', '\n', text)
    text = re.sub(r'\nCHAPTER \d+\n', '\n', text)
    
    # Remove weird syntax warnings or artifacts if they made it into the dump
    text = re.sub(r'Syntax Warning:.*', '', text)
    
    # Merge broken lines that aren't paragraph breaks
    # This is tricky in PDFs. Usually, a single newline is a continued line.
    # A double newline (or block break) is a paragraph.
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
            # If the current line doesn't start with a capital or follows a period,
            # it's likely a continuation.
            # Using a simpler heuristic: just merge lines unless they are very short or the previous ends in "."
            current_para += " " + line
        else:
            current_para = line
            
    if current_para:
        cleaned_lines.append(current_para)
        
    return "\n\n".join(cleaned_lines)

def extract_charms(pdf_path):
    print(f"Dumping PDF to text from {pdf_path}...")
    cmd = ["pdftotext", pdf_path, "-"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("Error running pdftotext")
        return

    text = result.stdout
    
    # Regex to find charm starts. 
    # Charms start with a name, then a "Cost:" line.
    # We'll use the Cost line as an anchor.
    # Pattern: [Name]\nCost: [Cost]; Mins: [Ability] [Dots], Essence [Dots]
    
    # Splitting by "Cost: " to find potential start points
    parts = re.split(r'\n(?=[^\n]+\nCost:)', text)
    
    charms_by_ability = {abil: [] for abil in ABILITIES}
    
    for part in parts:
        # Each part starts with the name, then Cost, then fields
        lines = [l.strip() for l in part.strip().split('\n') if l.strip()]
        if len(lines) < 5:
            continue
            
        name = lines[0]
        cost_line = lines[1]
        
        if not cost_line.startswith("Cost:"):
            continue
            
        # Parse fields
        # Ex: Cost: 3m; Mins: Athletics 3, Essence 1
        mins_match = re.search(r'Mins: ([\w\s]+) (\d+), Essence (\d+)', cost_line)
        if not mins_match:
            continue
            
        ability = mins_match.group(1).strip()
        min_ability = int(mins_match.group(2))
        min_essence = int(mins_match.group(3))
        
        if ability not in ABILITIES:
            continue
            
        cost_str = re.search(r'Cost: (.*?);', cost_line)
        cost = cost_str.group(1) if cost_str else cost_line.replace("Cost: ", "").split(';')[0]
        
        # Remaining headers
        type_val = ""
        keywords = ""
        duration = ""
        prereqs = []
        desc_start_idx = 2
        
        for i in range(2, min(len(lines), 7)):
            line = lines[i]
            if line.startswith("Type:"):
                type_val = line.replace("Type:", "").strip()
                desc_start_idx = i + 1
            elif line.startswith("Keywords:"):
                keywords = line.replace("Keywords:", "").strip()
                desc_start_idx = i + 1
            elif line.startswith("Duration:"):
                duration = line.replace("Duration:", "").strip()
                desc_start_idx = i + 1
            elif line.startswith("Prerequisite Charms:"):
                p_text = line.replace("Prerequisite Charms:", "").strip()
                if p_text and p_text.lower() != "none":
                    prereqs = [p.strip() for p in p_text.split(',')]
                desc_start_idx = i + 1
        
        # Merge description
        description_raw = "\n".join(lines[desc_start_idx:])
        # Note: If the next "part" starts with the NEXT charm, we might have over-captured.
        # But our split handled that. Wait, we might have noise at the end of the description
        # from page headers/footers.
        
        full_text = clean_description(description_raw)
        
        charm_data = {
            "name": name,
            "ability": ability,
            "minAbility": min_ability,
            "minEssence": min_essence,
            "prerequisites": prereqs,
            "cost": cost,
            "type": type_val,
            "keywords": keywords,
            "duration": duration,
            "fullText": full_text
        }
        
        charms_by_ability[ability].append(charm_data)
        print(f"Extracted: {name} ({ability})")

    # Save to files
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
        
    for ability, charms in charms_by_ability.items():
        if not charms:
            continue
            
        file_path = os.path.join(OUTPUT_DIR, f"{ability.lower().replace(' ', '-')}.json")
        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(charms, f, indent=4, ensure_ascii=False)
        print(f"Saved {len(charms)} charms to {file_path}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 extract_charms.py <path_to_pdf>")
        sys.exit(1)
        
    pdf_path = sys.argv[1]
    if not os.path.exists(pdf_path):
        print(f"Error: File not found: {pdf_path}")
        sys.exit(1)
        
    extract_charms(pdf_path)
