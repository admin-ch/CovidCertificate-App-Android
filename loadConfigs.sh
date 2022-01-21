#!/bin/bash

apps=("wallet" "verifier")
environments=("prod" "abn" "dev")

timestamp=$(date +%s%N | cut -b1-13)

for app in ${apps[@]}; do
  for environment in ${environments[@]}; do
    output="${app}/src/${environment}/assets/faq/config.json"
    if [[ $environment = "prod" ]]; then
      subdomain="cc-a" # Take the prod config from the ABN environment
    elif [ $environment = "abn" ]; then
      subdomain="cc-a"
    elif [ $environment = "dev" ]; then
      subdomain="cc-d"
    fi

    versionName=$(cat "${app}/build.gradle" | sed -n -e 's/^.*versionName\s*"\(.*\)"$/\1/p')
    url="https://www.${subdomain}.bit.admin.ch/app/${app}/v1/config?appversion=android-${versionName}&buildnr=${timestamp}"

    echo "Loading fallback ${environment} config for ${app} from ${url}"
    curl $url | jq > $output
  done
done
