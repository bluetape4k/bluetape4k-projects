package io.bluetape4k.feign.services

import java.io.Serializable

data class Post(
    val userId: Int,
    val id: Int,
    var title: String?,
    var body: String?,
): Serializable

data class Comment(
    val postId: Int,
    val id: Int,
    val name: String,
    val email: String,
    val body: String,
): Serializable

data class User(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val address: Address,
    val phone: String,
    val website: String,
    val company: Company,
): Serializable

data class Address(
    val street: String,
    val suite: String,
    val city: String,
    val zipcode: String,
    val geo: Geo,
): Serializable

data class Geo(val lat: Double, val lng: Double): Serializable

data class Company(val name: String, val catchPhrase: String, val bs: String): Serializable

data class Todo(
    val userId: Int,
    val id: Int,
    val title: String,
    val completed: Boolean,
): Serializable

data class Album(
    val userId: Int,
    val id: Int,
    val title: String,
): Serializable

data class Photo(
    val albumId: Int,
    val id: Int,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
): Serializable
