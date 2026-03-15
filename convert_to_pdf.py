#!/usr/bin/env python3
"""
Enterprise-grade Markdown to PDF converter
Creates professional PDFs with custom styling, table of contents, and metadata
"""

import os
import sys
import re
from pathlib import Path
import markdown
from weasyprint import HTML, CSS
from weasyprint.text.fonts import FontConfiguration
from datetime import datetime

def create_enhanced_css():
    """Create enhanced CSS with better styling"""
    return """
/* Enterprise Grade PDF Styling */
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
  --table-border: #d1d5db;
}

@page {
  size: A4;
  margin: 0.8in;

  @top-left {
    content: "Engineering Excellence Guide";
    font-family: 'Inter', sans-serif;
    font-size: 9pt;
    color: var(--text-secondary);
  }

  @top-right {
    content: "Page " counter(page);
    font-family: 'Inter', sans-serif;
    font-size: 9pt;
    color: var(--text-secondary);
  }

  @bottom-center {
    content: "© 2025 - Confidential Document";
    font-family: 'Inter', sans-serif;
    font-size: 8pt;
    color: var(--text-secondary);
    border-top: 1px solid var(--border-color);
    padding-top: 10px;
  }
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  line-height: 1.6;
  color: var(--text-primary);
  background-color: var(--background);
  margin: 0;
  padding: 0;
  font-size: 11pt;
}

/* Typography */
h1, h2, h3, h4, h5, h6 {
  font-weight: 600;
  line-height: 1.3;
  color: var(--primary-color);
  margin-top: 1.5em;
  margin-bottom: 0.8em;
  page-break-after: avoid;
}

h1 {
  font-size: 24pt;
  font-weight: 700;
  border-bottom: 3px solid var(--primary-color);
  padding-bottom: 0.5em;
  margin-bottom: 1.5em;
  page-break-before: always;
}

h1:first-of-type {
  page-break-before: avoid;
  text-align: center;
  margin-top: 0;
  margin-bottom: 2em;
  font-size: 28pt;
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
  margin-top: 1.2em;
}

h4 {
  font-size: 12pt;
  color: var(--text-primary);
  margin-top: 1em;
}

h5, h6 {
  font-size: 11pt;
  color: var(--text-secondary);
  margin-top: 0.8em;
}

/* Paragraphs and Text */
p {
  margin-bottom: 1em;
  text-align: justify;
  orphans: 2;
  widows: 2;
}

/* Lists */
ul, ol {
  padding-left: 1.5em;
  margin-bottom: 1em;
}

li {
  margin-bottom: 0.3em;
}

/* Code and Pre */
code {
  background-color: var(--code-bg);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
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
  page-break-inside: avoid;
  font-size: 9pt;
}

pre code {
  background: none;
  padding: 0;
  border-radius: 0;
}

/* Tables */
table {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
  font-size: 9pt;
  page-break-inside: avoid;
}

th, td {
  border: 1px solid var(--table-border);
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

/* Blockquotes */
blockquote {
  border-left: 4px solid var(--accent-color);
  margin: 1em 0;
  padding: 1em 1.5em;
  background-color: #f0fdf4;
  font-style: italic;
  page-break-inside: avoid;
}

/* Links */
a {
  color: var(--secondary-color);
  text-decoration: none;
}

/* Images */
img {
  max-width: 100%;
  height: auto;
  margin: 1em 0;
  page-break-inside: avoid;
}

/* Special styling for YAML */
.codehilite .language-yaml,
.highlight .language-yaml {
  background-color: #fffbeb;
  border-left: 4px solid #f59e0b;
}

/* Table of Contents */
.toc {
  background-color: #f8fafc;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 1.5em;
  margin: 2em 0;
  page-break-inside: avoid;
}

.toc h2 {
  font-size: 16pt;
  margin-top: 0;
  margin-bottom: 1em;
  text-align: left;
  border-left: none;
  padding-left: 0;
  margin-left: 0;
}

.toc ul {
  list-style-type: none;
  padding-left: 0;
}

.toc > ul > li {
  margin-bottom: 0.5em;
  font-weight: 500;
}

.toc ul ul {
  padding-left: 1.5em;
  margin-top: 0.2em;
}

/* Cover Page */
.cover-page {
  text-align: center;
  page-break-after: always;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 80vh;
  padding: 2em;
}

.cover-page h1 {
  font-size: 32pt;
  margin-bottom: 1em;
  border-bottom: none;
  page-break-before: avoid;
  color: var(--primary-color);
}

.cover-page .subtitle {
  font-size: 16pt;
  color: var(--text-secondary);
  margin-bottom: 3em;
  font-weight: 400;
}

.cover-page .metadata {
  font-size: 12pt;
  color: var(--text-primary);
  margin-top: 4em;
  border-top: 2px solid var(--border-color);
  padding-top: 2em;
}

/* Page breaks */
.page-break {
  page-break-before: always;
}

.avoid-break {
  page-break-inside: avoid;
}
"""

def preprocess_markdown(content, title):
    """Preprocess markdown content for better PDF conversion"""

    # Add cover page
    cover_page = f"""
<div class="cover-page">
    <h1>{title}</h1>
    <div class="subtitle">Enterprise Engineering Guide</div>
    <div class="metadata">
        <p><strong>Document Type:</strong> Technical Reference Guide</p>
        <p><strong>Target Audience:</strong> Principal Engineers, Software Architects, CTOs</p>
        <p><strong>Generated:</strong> {datetime.now().strftime('%B %d, %Y')}</p>
        <p><strong>Version:</strong> 1.0</p>
    </div>
</div>

"""

    # Process content
    processed_content = content

    # Improve table of contents
    if "## Table of Contents" in processed_content:
        toc_start = processed_content.find("## Table of Contents")
        toc_end = processed_content.find("\n## ", toc_start + 1)
        if toc_end == -1:
            toc_end = processed_content.find("\n# ", toc_start + 1)

        if toc_end != -1:
            toc_section = processed_content[toc_start:toc_end]
            toc_section = toc_section.replace("## Table of Contents", '<div class="toc"><h2>Table of Contents</h2>')
            toc_section += '</div>'
            processed_content = processed_content[:toc_start] + toc_section + processed_content[toc_end:]

    # Add page breaks before major sections
    processed_content = re.sub(r'\n(# [^#])', r'\n<div class="page-break"></div>\n\1', processed_content)

    # Improve code block styling
    processed_content = re.sub(r'```yaml\n', '```yaml\n', processed_content)

    return cover_page + processed_content

def convert_md_to_pdf(md_file_path, output_dir=None):
    """Convert a markdown file to enterprise-grade PDF"""

    md_path = Path(md_file_path)
    if not md_path.exists():
        print(f"Error: File {md_file_path} not found")
        return False

    if output_dir is None:
        output_dir = md_path.parent
    else:
        output_dir = Path(output_dir)
        output_dir.mkdir(exist_ok=True)

    # Generate output filename
    pdf_filename = md_path.stem + "_Enterprise.pdf"
    pdf_path = output_dir / pdf_filename

    print(f"Converting {md_path.name} to {pdf_filename}...")

    try:
        # Read markdown content
        with open(md_path, 'r', encoding='utf-8') as f:
            md_content = f.read()

        # Extract title from first heading
        title_match = re.search(r'^# (.+)$', md_content, re.MULTILINE)
        title = title_match.group(1) if title_match else md_path.stem.replace('_', ' ').title()

        # Preprocess markdown
        md_content = preprocess_markdown(md_content, title)

        # Convert markdown to HTML
        md = markdown.Markdown(extensions=[
            'toc',
            'tables',
            'fenced_code',
            'codehilite',
            'attr_list',
            'def_list',
            'footnotes',
            'md_in_html'
        ], extension_configs={
            'toc': {
                'title': 'Table of Contents',
                'toc_depth': '1-3'
            },
            'codehilite': {
                'css_class': 'highlight'
            }
        })

        html_content = md.convert(md_content)

        # Create complete HTML document
        html_doc = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{title}</title>
    <style>
    {create_enhanced_css()}
    </style>
</head>
<body>
    {html_content}
</body>
</html>
"""

        # Configure fonts
        font_config = FontConfiguration()

        # Convert to PDF
        html_obj = HTML(string=html_doc, base_url=str(md_path.parent))
        css_obj = CSS(string=create_enhanced_css(), font_config=font_config)

        html_obj.write_pdf(
            str(pdf_path),
            stylesheets=[css_obj],
            font_config=font_config
        )

        print(f"✅ Successfully created: {pdf_path}")
        return True

    except Exception as e:
        print(f"❌ Error converting {md_path.name}: {str(e)}")
        return False

def main():
    """Main function to convert all markdown files in the current directory"""

    current_dir = Path.cwd()
    md_files = list(current_dir.glob("*.md"))

    if not md_files:
        print("No markdown files found in the current directory")
        return

    print(f"Found {len(md_files)} markdown files to convert...")
    print("=" * 60)

    success_count = 0
    for md_file in md_files:
        if convert_md_to_pdf(md_file):
            success_count += 1
        print("-" * 40)

    print("=" * 60)
    print(f"Conversion complete: {success_count}/{len(md_files)} files converted successfully")

if __name__ == "__main__":
    main()