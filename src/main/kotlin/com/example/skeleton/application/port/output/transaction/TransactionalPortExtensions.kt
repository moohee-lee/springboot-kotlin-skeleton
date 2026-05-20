@file:Suppress("LongParameterList")

package com.example.skeleton.application.port.output.transaction

import com.example.skeleton.common.extensions.*

suspend fun <T1> TransactionalPort.executeAndReturn(block1: () -> T1): T1 = execute { block1() }

suspend fun <T1, T2> TransactionalPort.executeAndReturn(block1: () -> T1, block2: () -> T2): Pair<T1, T2> =
    execute { block1() to block2() }

suspend fun <T1, T2, T3> TransactionalPort.executeAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
): Triple<T1, T2, T3> = execute { Triple(block1(), block2(), block3()) }

suspend fun <T1, T2, T3, T4> TransactionalPort.executeAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
): Tuple4<T1, T2, T3, T4> = execute { Tuple4(block1(), block2(), block3(), block4()) }

suspend fun <T1, T2, T3, T4, T5> TransactionalPort.executeAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
    block5: () -> T5,
): Tuple5<T1, T2, T3, T4, T5> = execute { Tuple5(block1(), block2(), block3(), block4(), block5()) }

suspend fun <T1, T2, T3, T4, T5, T6> TransactionalPort.executeAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
    block5: () -> T5,
    block6: () -> T6,
): Tuple6<T1, T2, T3, T4, T5, T6> = execute {
    Tuple6(block1(), block2(), block3(), block4(), block5(), block6())
}

suspend fun <T1, T2, T3, T4, T5, T6, T7> TransactionalPort.executeAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
    block5: () -> T5,
    block6: () -> T6,
    block7: () -> T7,
): Tuple7<T1, T2, T3, T4, T5, T6, T7> = execute {
    Tuple7(block1(), block2(), block3(), block4(), block5(), block6(), block7())
}

suspend fun <T1, T2, T3, T4, T5, T6, T7, T8> TransactionalPort.executeAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
    block5: () -> T5,
    block6: () -> T6,
    block7: () -> T7,
    block8: () -> T8,
): Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> = execute {
    Tuple8(block1(), block2(), block3(), block4(), block5(), block6(), block7(), block8())
}

suspend fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> TransactionalPort.executeAndReturn(
    block1: () -> T1,
    block2: () -> T2,
    block3: () -> T3,
    block4: () -> T4,
    block5: () -> T5,
    block6: () -> T6,
    block7: () -> T7,
    block8: () -> T8,
    block9: () -> T9,
): Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> = execute {
    Tuple9(block1(), block2(), block3(), block4(), block5(), block6(), block7(), block8(), block9())
}
