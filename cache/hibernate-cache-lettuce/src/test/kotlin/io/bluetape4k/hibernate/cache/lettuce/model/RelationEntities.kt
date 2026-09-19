package io.bluetape4k.hibernate.cache.lettuce.model

import io.bluetape4k.support.hashOf
import jakarta.persistence.Cacheable
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable

@Entity
@Table(name = "departments")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Department: Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var name: String = ""

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    val employees: MutableSet<Employee> = linkedSetOf()

    fun addEmployee(employee: Employee) {
        employees += employee
        employee.department = this
    }

    override fun equals(other: Any?): Boolean =
        other is Department && id == other.id && name == other.name

    override fun hashCode(): Int =
        id?.hashCode() ?: hashOf(name)

    override fun toString(): String =
        "Department(id=$id, name='$name')"
}

@Entity
@Table(name = "employees")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Employee: Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var name: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    var department: Department? = null

    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    val projects: MutableSet<Project> = linkedSetOf()

    override fun equals(other: Any?): Boolean =
        other is Employee && id == other.id && name == other.name && department == other.department

    override fun hashCode(): Int =
        id?.hashCode() ?: hashOf(name, department)

    override fun toString(): String =
        "Employee(id=$id, name='$name', department=$department)"
}

@Entity
@Table(name = "projects")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Project: Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var title: String = ""

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "employee_projects",
        joinColumns = [JoinColumn(name = "project_id")],
        inverseJoinColumns = [JoinColumn(name = "employee_id")]
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    val members: MutableSet<Employee> = linkedSetOf()

    fun addMember(employee: Employee) {
        members += employee
        employee.projects += this
    }

    override fun equals(other: Any?): Boolean =
        other is Project && id == other.id && title == other.title

    override fun hashCode(): Int =
        id?.hashCode() ?: hashOf(title)

    override fun toString(): String =
        "Project(id=$id, title='$title')"
}
