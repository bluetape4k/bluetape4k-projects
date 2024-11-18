package io.bluetape4k.hibernate.mapping.associations.manytoone

import org.springframework.data.jpa.repository.JpaRepository

interface BeerRepository: JpaRepository<Beer, Int>
interface BreweryRepository: JpaRepository<Brewery, Int>

interface JugRepository: JpaRepository<Jug, Int>
interface JugMeterRepository: JpaRepository<JugMeter, Int>

interface SalesGuyRepository: JpaRepository<SalesGuy, Int>
interface SalesForceRepository: JpaRepository<SalesForce, Int>
