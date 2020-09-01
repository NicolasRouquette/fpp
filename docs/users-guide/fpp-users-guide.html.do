#!/bin/sh -e

# ----------------------------------------------------------------------
# fpp-users-guide.html.do
# ----------------------------------------------------------------------

redo-ifchange spell
redo-ifchange check
redo-ifchange undefined-tags.annotated.txt code-prettify
asciidoctor -n fpp-users-guide.adoc -o tmp.html
sed 's;https://cdnjs.cloudflare.com/ajax/libs/prettify/r298/run_prettify.min.js;code-prettify/run_prettify.js;' < tmp.html > $3
rm tmp.html