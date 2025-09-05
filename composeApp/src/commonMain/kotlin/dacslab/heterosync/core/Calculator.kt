package dacslab.heterosync.core

class Calculator {
    fun add(a: Double, b: Double): Double {
        return a + b
    }
    
    fun add(a: Int, b: Int): Int {
        return a + b
    }
    
    fun formatResult(result: Double): String {
        return if (result == result.toInt().toDouble()) {
            result.toInt().toString()
        } else {
            "%.2f".format(result)
        }
    }
    
    fun formatResult(result: Int): String {
        return result.toString()
    }
}