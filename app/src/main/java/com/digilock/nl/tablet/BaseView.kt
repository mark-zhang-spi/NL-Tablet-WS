package com.digilock.nl.tablet

interface BaseView<T> {

    var isActive: Boolean

    var presenter: T
}