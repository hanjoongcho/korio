@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.korio.ds.LinkedList2
import java.io.Closeable
import java.util.concurrent.ConcurrentLinkedQueue

class Signal<T>(val onRegister: () -> Unit = {}) : AsyncSequence<T> {
	internal val onceHandlers = ConcurrentLinkedQueue<(T) -> Unit>()

	inner class Node(val once: Boolean, val item: (T) -> Unit) : LinkedList2.Node<Node>(), Closeable {
		override fun close() {
			handlers.remove(this)
		}
	}

	private var handlers = LinkedList2<Node>()

	fun once(handler: (T) -> Unit): Closeable = _add(true, handler)
	fun add(handler: (T) -> Unit): Closeable = _add(false, handler)

	private fun _add(once: Boolean, handler: (T) -> Unit): Closeable {
		onRegister()
		val node = Node(once, handler)
		handlers.add(node)
		return node
	}

	operator fun invoke(value: T) {
		EventLoop.queue {
			val it = handlers.iterator()
			while (it.hasNext()) {
				val node = it.next()
				if (node.once) it.remove()
				node.item(value)
			}
		}
	}

	operator fun invoke(value: (T) -> Unit): Closeable = add(value)

	override fun iterator(): AsyncIterator<T> = asyncGenerate {
		while (true) {
			yield(waitOne())
		}
	}.iterator()
}

operator fun Signal<Unit>.invoke() = invoke(Unit)

suspend fun <T> Signal<T>.waitOne(): T = suspendCancellableCoroutine { c ->
	var close: Closeable? = null
	close = once {
		close?.close()
		c.resume(it)
	}
	c.onCancel {
		close?.close()
	}
}
