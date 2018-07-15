# Do not edit. bazel-deps autogenerates this file from dependencies.yaml.
def _jar_artifact_impl(ctx):
    jar_name = "%s.jar" % ctx.name
    ctx.download(
        output=ctx.path("jar/%s" % jar_name),
        url=ctx.attr.urls,
        sha256=ctx.attr.sha256,
        executable=False
    )
    src_name="%s-sources.jar" % ctx.name
    srcjar_attr=""
    has_sources = len(ctx.attr.src_urls) != 0
    if has_sources:
        ctx.download(
            output=ctx.path("jar/%s" % src_name),
            url=ctx.attr.src_urls,
            sha256=ctx.attr.src_sha256,
            executable=False
        )
        srcjar_attr ='\n    srcjar = ":%s",' % src_name

    build_file_contents = """
package(default_visibility = ['//visibility:public'])
java_import(
    name = 'jar',
    jars = ['{jar_name}'],{srcjar_attr}
)
filegroup(
    name = 'file',
    srcs = [
        '{jar_name}',
        '{src_name}'
    ],
    visibility = ['//visibility:public']
)\n""".format(jar_name = jar_name, src_name = src_name, srcjar_attr = srcjar_attr)
    ctx.file(ctx.path("jar/BUILD"), build_file_contents, False)
    return None

jar_artifact = repository_rule(
    attrs = {
        "artifact": attr.string(mandatory = True),
        "sha256": attr.string(mandatory = True),
        "urls": attr.string_list(mandatory = True),
        "src_sha256": attr.string(mandatory = False, default=""),
        "src_urls": attr.string_list(mandatory = False, default=[]),
    },
    implementation = _jar_artifact_impl
)

def jar_artifact_callback(hash):
    src_urls = []
    src_sha256 = ""
    source=hash.get("source", None)
    if source != None:
        src_urls = [source["url"]]
        src_sha256 = source["sha256"]
    jar_artifact(
        artifact = hash["artifact"],
        name = hash["name"],
        urls = [hash["url"]],
        sha256 = hash["sha256"],
        src_urls = src_urls,
        src_sha256 = src_sha256
    )
    native.bind(name = hash["bind"], actual = hash["actual"])

def list_dependencies():
    return [
    {"artifact": "com.google.code.gson:gson:2.8.2", "lang": "java", "sha1": "3edcfe49d2c6053a70a2a47e4e1c2f94998a49cf", "sha256": "b7134929f7cc7c04021ec1cc27ef63ab907e410cf0588e397b8851181eb91092", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.8.2/gson-2.8.2.jar", "source": {"sha1": "b2da9f8444128651758719856de579eacff7f387", "sha256": "1c291a2fe0867d66ef86832e014889a398a5c5b8e823206324a782b212df0df3", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.8.2/gson-2.8.2-sources.jar"} , "name": "com_google_code_gson_gson", "actual": "@com_google_code_gson_gson//jar", "bind": "jar/com/google/code/gson/gson"},
    {"artifact": "com.google.dagger:dagger:2.15", "lang": "java", "sha1": "13cc1f509deda05c1fe5a315519d7cb743b8333b", "sha256": "1f14720ffc3152a4207e374edb2ce114d94625058a6ef48a35cb67764dac4756", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/google/dagger/dagger/2.15/dagger-2.15.jar", "source": {"sha1": "374ed51e36e6af05ce1c7e688a5dc06be4d95b95", "sha256": "eb72948b541d1f7c40472faa6a7182395b41d77b23ddf7b40ffbfe2c28676d89", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/com/google/dagger/dagger/2.15/dagger-2.15-sources.jar"} , "name": "com_google_dagger_dagger", "actual": "@com_google_dagger_dagger//jar", "bind": "jar/com/google/dagger/dagger"},
    {"artifact": "javax.inject:javax.inject:1", "lang": "java", "sha1": "6975da39a7040257bd51d21a231b76c915872d38", "sha256": "91c77044a50c481636c32d916fd89c9118a72195390452c81065080f957de7ff", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/javax/inject/javax.inject/1/javax.inject-1.jar", "source": {"sha1": "a00123f261762a7c5e0ec916a2c7c8298d29c400", "sha256": "c4b87ee2911c139c3daf498a781967f1eb2e75bc1a8529a2e7b328a15d0e433e", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/javax/inject/javax.inject/1/javax.inject-1-sources.jar"} , "name": "javax_inject_javax_inject", "actual": "@javax_inject_javax_inject//jar", "bind": "jar/javax/inject/javax_inject"},
    ]

def maven_dependencies(callback = jar_artifact_callback):
    for hash in list_dependencies():
        callback(hash)
