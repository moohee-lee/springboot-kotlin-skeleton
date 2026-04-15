package com.example.skeleton.common.config

import com.example.skeleton.common.utils.SensitiveDataMasker
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.PropertySource
import org.springframework.stereotype.Component

/**
 * 기동 시점에 실제로 로드된 property source 를 우선순위 순서대로 출력한다.
 *
 * classpath yaml 뿐 아니라 ConfigMap, Secret, Vault, config tree 처럼
 * 외부에서 주입되는 source 도 함께 출력한다. source 이름을 화이트리스트로 좁히면
 * 새 source 종류가 늘어날 때마다 조용히 누락되므로, 잡음이 큰 시스템 source 만 제외한다.
 *
 * @author MooHee Lee
 */
@Component
@ConditionalOnProperty(
    prefix = "app.config",
    name = ["print-properties-on-startup"],
    havingValue = "true",
    matchIfMissing = false, // 설정 없으면 동작 안 함 (default false)
)
class LoadedPropertiesPrinter(
    private val environment: ConfigurableEnvironment,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun printLoadedProperties() {
        val candidates = environment.propertySources.filterNot(::isExcluded)
        val ownerByKey = resolveOwnerByKey(candidates)
        val printable = when {
            includeSystemSources() -> candidates
            else -> candidates.filterNot(::isSystemSource)
        }

        log.info("===== Loaded properties =====")
        log.info("activeProfiles={}", environment.activeProfiles.joinToString().ifBlank { "(default)" })

        if (printable.isEmpty()) {
            log.info("  (출력 대상 property source 가 없습니다)")
        }
        printable.forEach { source -> printSource(source, ownerByKey) }

        log.info("===== end =====")
    }

    private fun printSource(source: PropertySource<*>, ownerByKey: Map<String, String>) {
        log.info("----- source: {} -----", source.name)

        if (source !is EnumerablePropertySource<*>) {
            log.info("  (enumerable 하지 않아 key 목록을 알 수 없음)")
            return
        }
        source.propertyNames.sorted().forEach { key -> printProperty(source, key, ownerByKey[key]) }
    }

    private fun printProperty(source: EnumerablePropertySource<*>, key: String, ownerName: String?) {
        val raw = SensitiveDataMasker.maskIfSensitive(key, source.getProperty(key)?.toString())
        val resolved = resolveDisplayValue(key)

        when {
            ownerName != null && ownerName != source.name ->
                log.info("  {}={}  (overridden by {})", key, raw, ownerName)

            raw != resolved ->
                log.info("  {}={}  (resolved={})", key, raw, resolved)

            else ->
                log.info("  {}={}", key, raw)
        }
    }

    /**
     * placeholder 미해석으로 기동 후 로깅이 죽지 않도록 예외를 흡수한다.
     * `${KUBERNETES_NAMESPACE}` 처럼 런타임에만 주입되는 값이 비어 있으면
     * [ConfigurableEnvironment.getProperty] 가 예외를 던진다.
     */
    private fun resolveDisplayValue(key: String): String = runCatching { environment.getProperty(key) }
        .fold(
            onSuccess = { SensitiveDataMasker.maskIfSensitive(key, it) },
            onFailure = { PLACEHOLDER_RESOLVE_FAILED },
        )

    /** key 별로 실제 적용되는(우선순위가 가장 높은) source 이름을 구한다. */
    private fun resolveOwnerByKey(sources: List<PropertySource<*>>): Map<String, String> {
        val ownerByKey = mutableMapOf<String, String>()
        sources.filterIsInstance<EnumerablePropertySource<*>>().forEach { source ->
            source.propertyNames.forEach { key -> ownerByKey.putIfAbsent(key, source.name) }
        }
        return ownerByKey
    }

    private fun includeSystemSources(): Boolean =
        environment.getProperty(INCLUDE_SYSTEM_SOURCES_KEY, Boolean::class.java, false)

    private fun isExcluded(source: PropertySource<*>): Boolean = source.name.lowercase().let { name ->
        EXCLUDED_SOURCE_NAMES.any { name.contains(it) }
    }

    private fun isSystemSource(source: PropertySource<*>): Boolean = source.name.lowercase().let { name ->
        SYSTEM_SOURCE_NAMES.any { name.contains(it) }
    }

    private companion object {
        const val INCLUDE_SYSTEM_SOURCES_KEY = "app.config.print-system-properties-on-startup"
        const val PLACEHOLDER_RESOLVE_FAILED = "(placeholder 해석 실패)"

        /** 다른 source 를 감싸기만 해서 중복 출력만 늘리는 source */
        val EXCLUDED_SOURCE_NAMES = setOf("configurationproperties", "random")

        /** 항목 수가 많아 기본 출력에서는 제외하는 JVM/OS source */
        val SYSTEM_SOURCE_NAMES = setOf(
            "systemproperties",
            "systemenvironment",
            "servletconfiginitparams",
            "servletcontextinitparams",
            "jndiproperties",
        )
    }
}
