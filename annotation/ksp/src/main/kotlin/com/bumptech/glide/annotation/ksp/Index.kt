package com.bumptech.glide.annotation.ksp

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Index(val modules: Array<String>)
