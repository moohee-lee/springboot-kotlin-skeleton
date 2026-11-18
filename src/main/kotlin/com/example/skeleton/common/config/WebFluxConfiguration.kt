package com.example.skeleton.common.config

import com.example.skeleton.common.constant.CommonConstant
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.reactor.netty.NettyServerCustomizer
import org.springframework.boot.webflux.error.ErrorAttributes
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.validation.DefaultMessageCodesResolver
import org.springframework.validation.MessageCodesResolver
import org.springframework.validation.Validator
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.reactive.config.ApiVersionConfigurer
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import reactor.netty.http.server.logging.AccessLog
import tools.jackson.core.io.JsonStringEncoder
import java.net.InetSocketAddress
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class WebFluxConfiguration(
    private val messageSource: MessageSource,
    private val corsProperties: CorsProperties,
) : WebFluxConfigurer {
    @Bean
    fun nettyAccessLogCustomizer(): NettyServerCustomizer = NettyServerCustomizer { server ->
        val logger = LoggerFactory.getLogger("reactor.netty.http.server.AccessLog")
        val timestamp = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH).withZone(ZoneOffset.UTC)
        server.accessLog(true) { args ->
            if (!logger.isInfoEnabled) return@accessLog null
            val remote = args.connectionInformation()?.remoteAddress() as? InetSocketAddress
            val method = args.method()?.toString()
            val status = args.status()?.toString()
            val bytes = args.contentLength().takeIf { it > 0 && method != "HEAD" && status != "304" }
            AccessLog.create(
                "{} - {} [{}] \"{} {} {}\" {} {} \"{}\" \"{}\"",
                remote?.hostString ?: "-", args.user() ?: "-", timestamp.format(Instant.now()),
                method ?: "-", logField(args.uri()), args.protocol() ?: "-", status ?: "-", bytes ?: "-",
                logField(args.requestHeader("Referer")), logField(args.requestHeader("User-Agent")),
            )
        }
    }

    private fun logField(value: CharSequence?): String =
        if (value.isNullOrEmpty()) "-" else buildString { JsonStringEncoder.getInstance().quoteAsString(value, this) }

    @Bean
    fun messageCodesResolver(): MessageCodesResolver = DefaultMessageCodesResolver().apply {
        setMessageCodeFormatter(DefaultMessageCodesResolver.Format.POSTFIX_ERROR_CODE)
    }

    @Bean
    fun globalErrorAttributes(messageCodesResolver: MessageCodesResolver): ErrorAttributes =
        GlobalErrorAttributes(messageCodesResolver)

    @Bean
    fun applicationValidator(): LocalValidatorFactoryBean = LocalValidatorFactoryBean().apply {
        setValidationMessageSource(messageSource)
    }

    override fun getValidator(): Validator = applicationValidator()

    override fun getMessageCodesResolver(): MessageCodesResolver = messageCodesResolver()

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(*corsProperties.allowedOrigins.toTypedArray())
            .allowedMethods(*corsProperties.allowedMethods.toTypedArray())
            .allowedHeaders(*corsProperties.allowedHeaders.toTypedArray())
            .allowCredentials(corsProperties.allowCredentials)
            .maxAge(corsProperties.maxAge)
    }

    override fun configureApiVersioning(configurer: ApiVersionConfigurer) {
        configurer
            .usePathSegment(1)
            .setVersionRequired(true)
    }

    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.defaultCodecs().maxInMemorySize(CommonConstant.MAX_BUFFER_SIZE)
    }
}
