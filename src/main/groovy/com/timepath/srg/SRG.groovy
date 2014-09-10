package com.timepath.srg

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
class SRG {

    static void main(String[] args) {
        def p = parse("https://raw.githubusercontent.com/MinecraftForge/FML/master/conf/joined.srg".toURL())
        p.apply(p.fields, "https://raw.githubusercontent.com/MinecraftForge/FML/master/conf/fields.csv".toURL())
        p.apply(p.methods, "https://raw.githubusercontent.com/MinecraftForge/FML/master/conf/methods.csv".toURL())
        println "done"
    }

    @CompileDynamic
    static SRG parse(URL u) {
        SRG s = new SRG()
        u.newReader().eachLine { String it ->
            def tokens = it.split(" ")*.replace('/', '.')
            s.handle[tokens.head()](*tokens.tail())
        }
        return s
    }

    void apply(Map<String, String> map, URL u) {
        u.newReader().eachLine(1) { String it ->
            final List<String> split = it.split(",", 4).collect { String token -> token.replace('/', '.') }
            def entry = map.find { it.value.contains(split[0]) }
            if (!entry) return
            entry.value = entry.value.replace(split[0], split[1])
        }
    }

    Map<String, String> packages = [:], classes = [:], fields = [:], methods = [:]

    private def handle = [
            "PK:": { String from, String to ->
                packages[from] = to
            },
            "CL:": { String from, String to ->
                classes[from] = to
            },
            "FD:": { String from, String to ->
                fields[from] = to
            },
            "MD:": { String from, String fromsigstr, String to, String tosigstr ->
                def fromsig = readSig fromsigstr
                def tosig = readSig tosigstr
                methods["${fromsig[-1]} ${readClass from}(${fromsig[0..<-1].join(", ")})".toString()] =
                        "${tosig[-1]} ${readClass to}(${tosig[0..<-1].join(", ")})".toString()
            }
    ]

    private static final PATTERN = /(\[*?(?:L[^;]*|[A-Z]))/

    private static Map<String, Closure<String>> TYPES = [
            Z: { "boolean" },
            B: { "byte" },
            C: { "char" },
            S: { "short" },
            I: { "int" },
            J: { "long" },
            F: { "float" },
            D: { "double" },
            V: { "void" },
            L: { String it -> readClass it.substring(1) },
    ].withDefault { return { it } }

    private static List<String> readSig(String sig) {
        (sig =~ PATTERN).collect { List it ->
            String token = it[0]
            def split = token.split "\\["
            token = split[-1]
            Closure<String> c = TYPES[token[0]]
            c.call(token) + "[]" * (split.size() - 1)
        }
    }

    private static String readClass(String s) { s.replace('/', '.') }

}
