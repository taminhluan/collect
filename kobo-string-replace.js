// Replace string occurrences with their Kobo equivalents
// run "npm install"
// and then "node string-replace.js"

var replace = require("replace");

replace({
    regex: "<string name=\"main_menu_details\">.*<",
    replacement: "<string name=\"main_menu_details\">Part of KoBoToolbox<",
    paths: ['collect_app/src/main/res'],
    recursive: true,
    silent: true
});

replace({
    regex: "<string name=\"click_to_web\">.*<",
    replacement: "<string name=\"click_to_web\">KoBoCollect is part of KoBoToolbox (kobotoolbox.org)\\n\\nKoBoCollect is based on the OpenDataKit<",
    paths: ['collect_app/src/main/res'],
    recursive: true,
    silent: true
});

replace({
    regex: "https://opendatakit.org",
    replacement: "http://www.kobotoolbox.org",
    paths: ['collect_app/src/main/java/org/koboc/collect/android/activities/AboutActivity.java'],
    recursive: true,
    silent: true
});

replace({
    regex: "ODK Aggregate",
    replacement: "KoBoToolbox",
    paths: ['collect_app/src/main/res'],
    recursive: true,
    silent: true
});

replace({
    regex: "ODK Collect",
    replacement: "KoBoCollect",
    paths: ['collect_app/src/main/res'],
    recursive: true,
    silent: true
});

replace({
    regex: 'Open Data Kit .ODK.',
    replacement: "KoBoToolbox",
    paths: ['collect_app/src/main/res'],
    recursive: true,
    silent: true
});

replace({
    regex: 'ODK',
    replacement: "KoBoToolbox",
    paths: ['collect_app/src/main/res'],
    recursive: true,
    silent: true
});

// This is likely too broad
// replace({
//     regex: "ODK",
//     replacement: "KoboToolbox",
//     paths: ['collect_app/src/main/res'],
//     recursive: true,
//     silent: true
// });

