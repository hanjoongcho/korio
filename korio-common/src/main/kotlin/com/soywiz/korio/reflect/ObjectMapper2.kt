package com.soywiz.korio.reflect

import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.ds.toLinkedMap
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.Dynamic
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

data class ClassReflect<T : Any>(
	val clazz: KClass<T>,
	val props: List<KProperty1<T, *>>,
	val types: List<KClass<*>>,
	val create: ObjectMapper2.Args.() -> Any?
) {
	val anyProps = props.map { it as KProperty1<Any?, Any?> }
	val propNames = anyProps.map { it.name }
	val propsByName = anyProps.map { it.name to it }.toMap()
	fun toMap(obj: T): Map<Any?, Any?> = anyProps.map { it.name to it.get(obj) }.toLinkedMap()
}

class ObjectMapper2 {
	val reflectByClass = lmapOf<KClass<*>, ClassReflect<*>>()

	class Args(val mapper: ObjectMapper2, val args: List<Any?>) {
		inline fun <reified T : Any> get(index: Int): T = get(T::class, index)
		fun <T : Any> get(clazz: KClass<T>, index: Int): T = args[index] as T
	}

	fun register(c: ClassReflect<*>) = this.apply {
		reflectByClass[c.clazz] = c
	}

	fun getKeys(obj: Any?): List<Any?> = when (obj) {
		null -> listOf()
		is Map<*, *> -> obj.keys.toList()
		is List<*> -> obj.indices.toList()
		else -> reflectByClass[obj::class]?.propNames ?: listOf()
	}

	fun getTypes(obj: Any?): List<KClass<*>> = when (obj) {
		null -> listOf()
		is Map<*, *> -> obj.keys.filterNotNull().map { it::class }
		//is List<*> -> obj.indices.map { Int::class }
		else -> getTypes(obj::class)
	}

	fun getTypes(clazz: KClass<*>): List<KClass<*>> = reflectByClass[clazz]?.types ?: listOf()

	fun get(obj: Any?, key: Any?): Any? = when (obj) {
		null -> null
		is Map<*, *> -> obj[Dynamic.toString(key)]
		is List<*> -> obj[Dynamic.toInt(key)]
		else -> reflectByClass[obj::class]?.propsByName?.get(key)?.get(obj)
	}

	fun set(obj: Any?, key: Any?, value: Any?): Unit = when (obj) {
		null -> Unit
		is MutableMap<*, *> -> {
			(obj as MutableMap<Any?, Any?>).set(Dynamic.toString(key), value)
			Unit
		}
		is MutableList<*> -> {
			(obj as MutableList<Any?>).set(Dynamic.toInt(key), value)
			Unit
		}
		else -> {
			(reflectByClass[obj::class]?.propsByName?.get(key) as? KMutableProperty1<Any?, Any?>?)?.set(obj, value)
			Unit
		}
	}

	inline fun <reified T : Any> create(map: Map<String, String>): T = create(T::class, map)

	fun <T : Any> create(clazz: KClass<T>, map: Map<String, Any?>): T {
		val classReflect = reflectByClass[clazz] ?: invalidOp("Unhandled $clazz")
		return classReflect.create(Args(this, classReflect.anyProps.map { map[it.name] })) as T
	}

	fun <T : Any> create(clazz: KClass<T>, args: List<Any?>): T {
		return reflectByClass[clazz]?.create?.invoke(Args(this, args)) as T
	}
}

val Mapper2 = ObjectMapper2()