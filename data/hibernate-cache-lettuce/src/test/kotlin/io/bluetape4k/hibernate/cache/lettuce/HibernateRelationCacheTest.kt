package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.model.Department
import io.bluetape4k.hibernate.cache.lettuce.model.Employee
import io.bluetape4k.hibernate.cache.lettuce.model.Project
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HibernateRelationCacheTest: AbstractHibernateNearCacheTest() {

    @BeforeEach
    fun clearCacheAndData() {
        sessionFactory.cache.evictAllRegions()
        sessionFactory.statistics.clear()
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.createNativeMutationQuery("DELETE FROM employee_projects").executeUpdate()
            session.createMutationQuery("DELETE FROM Project").executeUpdate()
            session.createMutationQuery("DELETE FROM Employee").executeUpdate()
            session.createMutationQuery("DELETE FROM Department").executeUpdate()
            session.transaction.commit()
        }
    }

    @Test
    fun `One-To-Many 연관 컬렉션이 2nd level cache 를 사용한다`() {
        val departmentId = sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val department = Department().apply { name = "Platform" }
            department.addEmployee(Employee().apply { name = "Alice" })
            department.addEmployee(Employee().apply { name = "Bob" })
            session.persist(department)
            session.transaction.commit()
            department.id!!
        }

        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val loaded = session.find(Department::class.java, departmentId)
            loaded.shouldNotBeNull()
            loaded.employees.size shouldBeEqualTo 2
            session.transaction.commit()
        }
        val hitAfterFirstLoad = sessionFactory.statistics.secondLevelCacheHitCount

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val loaded = session.find(Department::class.java, departmentId)
            loaded.shouldNotBeNull()
            loaded.employees.size shouldBeEqualTo 2
            session.transaction.commit()
        }

        sessionFactory.statistics.secondLevelCacheHitCount shouldBeGreaterThan hitAfterFirstLoad
    }

    @Test
    fun `Many-To-One 연관 참조가 2nd level cache 를 사용한다`() {
        val employeeId = sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val department = Department().apply { name = "Data" }
            val employee = Employee().apply { name = "Charlie" }
            department.addEmployee(employee)
            session.persist(department)
            session.transaction.commit()
            employee.id!!
        }

        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val loaded = session.find(Employee::class.java, employeeId)
            loaded.shouldNotBeNull()
            loaded.department!!.name shouldBeEqualTo "Data"
            session.transaction.commit()
        }
        val hitAfterFirstLoad = sessionFactory.statistics.secondLevelCacheHitCount

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val loaded = session.find(Employee::class.java, employeeId)
            loaded.shouldNotBeNull()
            loaded.department!!.name shouldBeEqualTo "Data"
            session.transaction.commit()
        }

        sessionFactory.statistics.secondLevelCacheHitCount shouldBeGreaterThan hitAfterFirstLoad
    }

    @Test
    fun `Many-To-Many 연관 컬렉션이 2nd level cache 를 사용한다`() {
        val projectId = sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val department = Department().apply { name = "Infra" }
            val employee1 = Employee().apply { name = "Dave" }
            val employee2 = Employee().apply { name = "Eve" }
            department.addEmployee(employee1)
            department.addEmployee(employee2)

            val project = Project().apply { title = "NearCache" }
            project.addMember(employee1)
            project.addMember(employee2)

            session.persist(department)
            session.persist(project)
            session.transaction.commit()
            project.id!!
        }

        sessionFactory.statistics.clear()

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val loaded = session.find(Project::class.java, projectId)
            loaded.shouldNotBeNull()
            loaded.members.size shouldBeEqualTo 2
            session.transaction.commit()
        }
        val hitAfterFirstLoad = sessionFactory.statistics.secondLevelCacheHitCount

        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val loaded = session.find(Project::class.java, projectId)
            loaded.shouldNotBeNull()
            loaded.members.size shouldBeEqualTo 2
            session.transaction.commit()
        }

        sessionFactory.statistics.secondLevelCacheHitCount shouldBeGreaterThan hitAfterFirstLoad
    }
}
