package io.bluetape4k.hibernate.mapping.associations.manytoone

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class ManyToOneTest @Autowired constructor(
    private val beerRepo: BeerRepository,
    private val breweryRepo: BreweryRepository,
    private val jugRepo: JugRepository,
    private val jugMeterRepo: JugMeterRepository,
    private val salesGuyRepo: SalesGuyRepository,
    private val salesForceRepo: SalesForceRepository,
): AbstractHibernateTest() {

    companion object: KLogging()

    @Test
    fun `many-to-one unidirectional`() {
        val jug = Jug("Jug Summer camp")
        val emmanuel = JugMeter("Emmanuel Bernard")
        emmanuel.memberOf = jug

        val jerome = JugMeter("Jerome")
        jerome.memberOf = jug

        jugRepo.save(jug)
        jugMeterRepo.save(emmanuel)
        jugMeterRepo.save(jerome)
        flushAndClear()

        val eloaded = jugMeterRepo.findByIdOrNull(emmanuel.id!!)!!
        eloaded shouldBeEqualTo emmanuel
        eloaded.memberOf shouldBeEqualTo jug

        val jloaded = jugMeterRepo.findByIdOrNull(jerome.id!!)!!
        jloaded shouldBeEqualTo jerome
        jloaded.memberOf shouldBeEqualTo jug

        jugMeterRepo.deleteAll()
        flushAndClear()

        // @ManyToOne의 cascade가 none 이고, unidirectional이기 때문에 Jug는 삭제되지 않는다.
        jugRepo.findAll().shouldNotBeEmpty()

        jugRepo.deleteById(jug.id!!)
        flushAndClear()
        jugRepo.findAll().shouldBeEmpty()
    }

    @Test
    fun `many-to-one bidirectional`() {
        val brewery = Brewery("Berlin")
        val beer1 = Beer("Normal")
        val beer2 = Beer("Black")
        val beer3 = Beer("ColdBrew")

        brewery.addBeers(beer1, beer2, beer3)

        // bidirectional 이므로, brewery를 저장하면, beer 들도 저장된다.
        breweryRepo.save(brewery)
        flushAndClear()

        val loaded = breweryRepo.findByIdOrNull(brewery.id!!)!!
        loaded shouldBeEqualTo brewery
        loaded.beers shouldContainSame listOf(beer1, beer2, beer3)

        // beer의 association을 끊고, brewery를 저장하면, orphant가 된 beer는 삭제된다. (orphanRemoval=true)
        val beerToRemove = loaded.beers.first()
        loaded.removeBeers(beerToRemove)
        breweryRepo.saveAndFlush(loaded)

        beerRepo.findAll().size shouldBeEqualTo 2

        breweryRepo.delete(loaded)
        breweryRepo.findAll().shouldBeEmpty()
        beerRepo.findAll().shouldBeEmpty()
    }

    @Test
    fun `many-to-one bidirectional with cascade all`() {
        val salesForce = SalesForce("BMW Korea")
        val salesGuy1 = SalesGuy("debop")
        val salesGuy2 = SalesGuy("smith")
        val salesGuy3 = SalesGuy("james")

        salesForce.addGuys(salesGuy1, salesGuy2, salesGuy3)

        // SalesForce를 저장하면 many에 해당하는 sales guy 들을 모두 저장한다
        salesForceRepo.saveAndFlush(salesForce)
        clear()

        val loaded = salesForceRepo.findByIdOrNull(salesForce.id!!)!!
        loaded shouldBeEqualTo salesForce
        loaded.salesGuys shouldContainSame listOf(salesGuy1, salesGuy2, salesGuy3)

        val guyToRemove = loaded.salesGuys.last()
        loaded.removeGuys(guyToRemove)

        // salesGuy를 삭제하기 전에 salesForce=null 을 설정해야 삭제가 진행됩니다.
        salesGuyRepo.delete(guyToRemove)
        flushAndClear()

        val loaded2 = salesForceRepo.findByIdOrNull(salesForce.id!!)!!
        loaded2 shouldBeEqualTo salesForce
        loaded2.salesGuys.size shouldBeEqualTo salesForce.salesGuys.size - 1
    }
}
