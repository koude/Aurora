#!/usr/bin/env python3
from pathlib import Path

path = Path("app/build.gradle.kts")
text = path.read_text(encoding="utf-8")

if "import java.io.IOException" not in text:
    # Insert near the other java.* imports if possible.
    marker = "import java.net.HttpURLConnection\n"
    if marker in text:
        text = text.replace(marker, marker + "import java.io.IOException\n", 1)
    else:
        text = "import java.io.IOException\n" + text

text = text.replace(
    'throw java.io.IOException("$outputFileName downloaded as an empty file")',
    'throw IOException("$outputFileName downloaded as an empty file")'
)

path.write_text(text, encoding="utf-8")
print("Fixed IOException reference in app/build.gradle.kts")
