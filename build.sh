#!/bin/bash

# install lein

if [[ ! -f ~/bin/lein ]]; then
    echo "lein not present! downloading ..."
    mkdir -p ~/bin
    wget --no-check-certificate  -O ~/bin/lein "https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein"
    chmod 755 ~/bin/lein
fi

~/bin/lein $@
