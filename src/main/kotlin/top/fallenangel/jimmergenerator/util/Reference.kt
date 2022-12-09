package top.fallenangel.jimmergenerator.util

class Reference<T : Any>() {
    lateinit var value: T

    constructor(value: T) : this() {
        this.value = value
    }
}
