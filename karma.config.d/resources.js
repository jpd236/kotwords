// Serve the resource files copied into the resources/ directory.
// See build.gradle under jsTestProcessResources.
config.files.push({
    "pattern": "resources/**",
    "served": true,
    "included": false
});
