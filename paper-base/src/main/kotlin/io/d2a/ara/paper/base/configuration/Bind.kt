package io.d2a.ara.paper.base.configuration

class Bind<T>(
    private val delegate: Configuration,
    val path: String,
    val default: T?,
) {

    fun get(): T? = delegate.get(path, default)

    fun set(value: T?) = delegate.set(path, value)

}