package com.example.skeleton.common.utils

/**
 * 로그에 노출되면 안 되는 값의 마스킹 규칙을 한 곳에서 관리한다.
 *
 * 마스킹 규칙
 * - 1~2자: 전체 마스킹
 * - 3자 이상: 첫 글자와 마지막 글자만 노출
 * - 마스킹 후에도 원본 길이를 유지한다
 *
 * @author MooHee Lee
 */
object SensitiveDataMasker {
    const val NULL_DISPLAY = "null"

    /**
     * 설정 키 판별용 키워드.
     *
     * 키에서 영문/숫자만 남긴 뒤 부분 문자열로 비교하므로
     * `api-key`, `api_key`, `apiKey` 를 `apikey` 하나로 커버한다.
     */
    val SENSITIVE_KEY_KEYWORDS: Set<String> = setOf(
        "password",
        "passwd",
        "pwd",
        "secret",
        "token",
        "credential",
        "apikey",
        "privatekey",
        "accesskey",
        "secretkey",
        "authorization",
    )

    private const val MASK = "*"
    private const val MIN_LENGTH_FOR_PARTIAL_REVEAL = 3
    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]")

    fun isSensitiveKey(key: String): Boolean {
        val normalized = key.lowercase().replace(NON_ALPHANUMERIC, "")
        return SENSITIVE_KEY_KEYWORDS.any { normalized.contains(it) }
    }

    fun mask(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        if (value.length < MIN_LENGTH_FOR_PARTIAL_REVEAL) return MASK.repeat(value.length)
        return "${value.first()}${MASK.repeat(value.length - 2)}${value.last()}"
    }

    /**
     * 키가 민감 키워드를 포함할 때만 값을 마스킹한다. null 값은 `"null"` 문자열로 표기한다.
     */
    fun maskIfSensitive(key: String, value: String?): String = when {
        value == null -> NULL_DISPLAY
        isSensitiveKey(key) -> mask(value)
        else -> value
    }
}
