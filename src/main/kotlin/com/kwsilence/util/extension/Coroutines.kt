package com.kwsilence.util.extension

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

fun launchIO(block: suspend CoroutineScope.() -> Unit): Job =
    CoroutineScope(Dispatchers.IO).launch(block = block)

fun launchDefault(block: suspend CoroutineScope.() -> Unit): Job =
    CoroutineScope(Dispatchers.Default).launch(block = block)

fun CoroutineScope.launchIO(block: suspend CoroutineScope.() -> Unit): Job =
    launch(Dispatchers.IO, block = block)

suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.IO, block)

suspend fun <T> newTransactionIO(block: suspend Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

suspend fun <T> newTransactionDefault(block: suspend Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.Default, statement = block)

suspend fun <T> asyncTransaction(block: suspend Transaction.() -> T): Deferred<T> =
    suspendedTransactionAsync(statement = block)