#!/usr/bin/env python3
"""
Simple but effective Markdown to PDF converter using pandoc
Creates professional PDFs with custom styling
"""

import os
import subprocess
import sys
from pathlib import Path
from datetime import datetime

def create_pandoc_template():
    """Create a custom HTML template for pandoc"""
    template = '''<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$title$</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

        :root {
            --primary-color: #1e3a8a;
            --secondary-color: #3b82f6;
            --accent-color: #10b981;
            --text-primary: #1f2937;
            --text-secondary: #6b7280;
            --background: #ffffff;
            --border-color: #e5e7eb;
            --code-bg: #f8fafc;
        }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            line-height: 1.6;
            color: var(--text-primary);
            max-width: 800px;
            margin: 0 auto;
            padding: 40px 20px;
            font-size: 11pt;
        }

        h1, h2, h3, h4, h5, h6 {
            font-weight: 600;
            line-height: 1.3;
            color: var(--primary-color);
            margin-top: 2em;
            margin-bottom: 0.8em;
        }

        h1 {
            font-size: 24pt;
            font-weight: 700;
            border-bottom: 3px solid var(--primary-color);
            padding-bottom: 0.5em;
            margin-bottom: 1.5em;
            text-align: center;
        }

        h2 {
            font-size: 18pt;
            color: var(--secondary-color);
            border-left: 4px solid var(--secondary-color);
            padding-left: 1em;
            margin-left: -1.2em;
        }

        h3 {
            font-size: 14pt;
            color: var(--text-primary);
        }

        h4 {
            font-size: 12pt;
            color: var(--text-primary);
        }

        p {
            margin-bottom: 1em;
            text-align: justify;
        }

        ul, ol {
            padding-left: 1.5em;
            margin-bottom: 1em;
        }

        li {
            margin-bottom: 0.3em;
        }

        code {
            background-color: var(--code-bg);
            padding: 0.2em 0.4em;
            border-radius: 3px;
            font-family: 'SF Mono', Monaco, monospace;
            font-size: 0.9em;
            color: var(--primary-color);
        }

        pre {
            background-color: var(--code-bg);
            border: 1px solid var(--border-color);
            border-radius: 6px;
            padding: 1em;
            overflow-x: auto;
            margin: 1em 0;
            font-size: 9pt;
        }

        pre code {
            background: none;
            padding: 0;
            border-radius: 0;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin: 1em 0;
            font-size: 9pt;
        }

        th, td {
            border: 1px solid var(--border-color);
            padding: 0.6em;
            text-align: left;
        }

        th {
            background-color: var(--primary-color);
            color: white;
            font-weight: 600;
        }

        tr:nth-child(even) {
            background-color: #f9fafb;
        }

        blockquote {
            border-left: 4px solid var(--accent-color);
            margin: 1em 0;
            padding: 1em 1.5em;
            background-color: #f0fdf4;
            font-style: italic;
        }

        a {
            color: var(--secondary-color);
            text-decoration: none;
        }

        img {
            max-width: 100%;
            height: auto;
            margin: 1em 0;
        }

        .toc {
            background-color: #f8fafc;
            border: 1px solid var(--border-color);
            border-radius: 6px;
            padding: 1.5em;
            margin: 2em 0;
        }

        .toc h2 {
            margin-top: 0;
            margin-bottom: 1em;
            text-align: left;
            border-left: none;
            padding-left: 0;
            margin-left: 0;
        }

        .cover-info {
            text-align: center;
            margin-bottom: 3em;
            padding: 2em;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-radius: 8px;
        }

        .cover-info h1 {
            color: white;
            border-bottom: none;
            margin-bottom: 1em;
        }

        .metadata {
            font-size: 10pt;
            color: var(--text-secondary);
            text-align: center;
            margin-top: 2em;
            padding-top: 1em;
            border-top: 1px solid var(--border-color);
        }

        @media print {
            body { padding: 0; }
            .cover-info { page-break-after: always; }
            h1, h2 { page-break-after: avoid; }
            h1 { page-break-before: always; }
            h1:first-of-type { page-break-before: avoid; }
        }
    </style>
</head>
<body>
    <div class="cover-info">
        <h1>$title$</h1>
        <p><strong>Enterprise Engineering Guide</strong></p>
        <p>For Principal Engineers, Software Architects, and CTOs</p>
        <p>Generated: $date$</p>
    </div>

    $if(toc)$
    <div class="toc">
        <h2>Table of Contents</h2>
        $toc$
    </div>
    $endif$

    $body$

    <div class="metadata">
        <p>© 2025 - Confidential Engineering Document</p>
        <p>This document contains proprietary technical information</p>
    </div>
</body>
</html>'''
    return template

def convert_md_to_html_pdf(md_file_path):
    """Convert markdown to HTML and then to PDF using pandoc"""

    md_path = Path(md_file_path)
    if not md_path.exists():
        print(f"❌ Error: File {md_file_path} not found")
        return False

    # Extract title from markdown
    try:
        with open(md_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Find first heading
        import re
        title_match = re.search(r'^# (.+)$', content, re.MULTILINE)
        title = title_match.group(1) if title_match else md_path.stem.replace('_', ' ').title()

    except Exception as e:
        print(f"❌ Error reading {md_path.name}: {e}")
        return False

    # Output paths
    html_filename = md_path.stem + "_Enterprise.html"
    pdf_filename = md_path.stem + "_Enterprise.pdf"
    template_filename = "template.html"

    html_path = md_path.parent / html_filename
    pdf_path = md_path.parent / pdf_filename
    template_path = md_path.parent / template_filename

    print(f"🔄 Converting {md_path.name}...")

    try:
        # Create template file
        with open(template_path, 'w', encoding='utf-8') as f:
            f.write(create_pandoc_template())

        # Convert MD to HTML with pandoc
        cmd = [
            "pandoc",
            str(md_path),
            "-f", "markdown",
            "-t", "html5",
            "--template", str(template_path),
            "--toc",
            "--toc-depth=3",
            "--standalone",
            "--metadata", f"title={title}",
            "--metadata", f"date={datetime.now().strftime('%B %d, %Y')}",
            "-o", str(html_path)
        ]

        result = subprocess.run(cmd, capture_output=True, text=True)

        if result.returncode != 0:
            print(f"❌ Pandoc error: {result.stderr}")
            return False

        print(f"✅ Created HTML: {html_filename}")

        # Clean up template
        template_path.unlink()

        # Print instructions for manual PDF creation
        print(f"📋 To create PDF: Open {html_filename} in browser and print to PDF as {pdf_filename}")
        print(f"   Or use: wkhtmltopdf --page-size A4 --margin-top 20mm --margin-bottom 20mm --margin-left 15mm --margin-right 15mm {html_filename} {pdf_filename}")

        return True

    except Exception as e:
        print(f"❌ Error converting {md_path.name}: {e}")
        # Clean up template if it exists
        if template_path.exists():
            template_path.unlink()
        return False

def main():
    """Convert all markdown files in current directory"""

    current_dir = Path.cwd()
    md_files = list(current_dir.glob("*.md"))

    if not md_files:
        print("❌ No markdown files found in current directory")
        return

    print(f"🔍 Found {len(md_files)} markdown files to convert")
    print("=" * 60)

    success_count = 0
    for md_file in md_files:
        if convert_md_to_html_pdf(md_file):
            success_count += 1
        print("-" * 40)

    print("=" * 60)
    print(f"✅ Conversion complete: {success_count}/{len(md_files)} files converted to HTML")
    print("\n📖 Next steps:")
    print("1. Open each HTML file in a web browser")
    print("2. Use browser's Print function (Ctrl/Cmd + P)")
    print("3. Select 'Save as PDF' as destination")
    print("4. Choose appropriate print settings (A4, margins, etc.)")
    print("\nAlternatively, install wkhtmltopdf for automated PDF conversion")

if __name__ == "__main__":
    main()