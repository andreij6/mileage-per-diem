package com.aijones.mileage.core.analytics.impl

internal object FlatJson {
    fun encode(map: Map<String, Any>): String = buildString {
        append('{')
        map.entries.forEachIndexed { i, (k, v) ->
            if (i > 0) append(',')
            appendString(k)
            append(':')
            when (v) {
                is Number, is Boolean -> append(v.toString())
                else -> appendString(v.toString())
            }
        }
        append('}')
    }

    fun decode(json: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        var i = json.indexOf('{') + 1
        fun skipWs() { while (i < json.length && json[i].isWhitespace()) i++ }
        fun readString(): String {
            val sb = StringBuilder()
            i++
            while (json[i] != '"') {
                if (json[i] == '\\') {
                    i++
                    sb.append(
                        when (json[i]) {
                            'n' -> '\n'
                            't' -> '\t'
                            'u' -> json.substring(i + 1, i + 5).toInt(16).toChar().also { i += 4 }
                            else -> json[i]
                        },
                    )
                } else {
                    sb.append(json[i])
                }
                i++
            }
            i++
            return sb.toString()
        }
        while (true) {
            skipWs()
            if (i >= json.length || json[i] == '}') break
            if (json[i] == ',') { i++; continue }
            val key = readString()
            skipWs()
            i++
            skipWs()
            val value = if (json[i] == '"') {
                readString()
            } else {
                val start = i
                while (i < json.length && json[i] != ',' && json[i] != '}') i++
                json.substring(start, i).trim()
            }
            out[key] = value
        }
        return out
    }

    private fun StringBuilder.appendString(s: String) {
        append('"')
        s.forEach { c ->
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c < ' ' -> append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> append(c)
            }
        }
        append('"')
    }
}
