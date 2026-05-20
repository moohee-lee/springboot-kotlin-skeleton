@file:Suppress("LongParameterList")

package com.example.skeleton.application.port.output.transaction

import com.example.skeleton.common.extensions.*

suspend fun <T1> TransactionalPort.executeReadOnlyAndReturn(block1: () -> T1): T1 = executeReadOnly { block1() }

suspend fun <T1, T2> TransactionalPort.executeReadOnlyAndReturn(block1: () -> T1, block2: () -> T2): Pair<T1, T2> =
    executeReadOnly { block1() to block2() }

suspend fun <T1, T2, T3> TransactionalPort.executeReadOnlyAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
): Triple<T1, T2, T3> = executeReadOnly { Triple(block1(), block2(), block3()) }

suspend fun <T1, T2, T3, T4> TransactionalPort.executeReadOnlyAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
): Tuple4<T1, T2, T3, T4> = executeReadOnly { Tuple4(block1(), block2(), block3(), block4()) }

suspend fun <T1, T2, T3, T4, T5> TransactionalPort.executeReadOnlyAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
    block5: () -> T5,
): Tuple5<T1, T2, T3, T4, T5> = executeReadOnly { Tuple5(block1(), block2(), block3(), block4(), block5()) }

suspend fun <T1, T2, T3, T4, T5, T6> TransactionalPort.executeReadOnlyAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
    block5: () -> T5,
    block6: () -> T6,
): Tuple6<T1, T2, T3, T4, T5, T6> = executeReadOnly {
    Tuple6(block1(), block2(), block3(), block4(), block5(), block6())
}

suspend fun <T1, T2, T3, T4, T5, T6, T7> TransactionalPort.executeReadOnlyAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
    block5: () -> T5,
    block6: () -> T6,
    block7: () -> T7,
): Tuple7<T1, T2, T3, T4, T5, T6, T7> = executeReadOnly {
    Tuple7(block1(), block2(), block3(), block4(), block5(), block6(), block7())
}

suspend fun <T1, T2, T3, T4, T5, T6, T7, T8> TransactionalPort.executeReadOnlyAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
    block5: () -> T5,
    block6: () -> T6,
    block7: () -> T7,
    block8: () -> T8,
): Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> = executeReadOnly {
    Tuple8(block1(), block2(), block3(), block4(), block5(), block6(), block7(), block8())
}

suspend fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> TransactionalPort.executeReadOnlyAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
    block5: () -> T5,
    block6: () -> T6,
    block7: () -> T7,
    block8: () -> T8,
    block9: () -> T9,
): Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> = executeReadOnly {
    Tuple9(block1(), block2(), block3(), block4(), block5(), block6(), block7(), block8(), block9())
}
