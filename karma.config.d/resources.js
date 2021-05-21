// Hack to make resources available to JS tests.
// JsTestRunner can fetch these from Karma's webserver.
// Works around https://youtrack.jetbrains.com/issue/KT-42923
config.set({
  basePath: '../../../../'
});
config.files.push({
    "pattern": "build/processedResources/js/**",
    "served": true,
    "included": false
});
