/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import java.util.Objects;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Jpa(annotatedClasses = {
		CorrelationCteDerivedRootTest.MyDepartment.class,
		CorrelationCteDerivedRootTest.MyEmployee.class
		}
)
@JiraKey("HHH-17522")
public class CorrelationCteDerivedRootTest {

	@BeforeAll
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {

			MyDepartment department = new MyDepartment("TEST DEPARTMENT");

			MyEmployee employee = new MyEmployee("TEST firts Name", "TEST last name", "test@mail.com",department);



			entityManager.persist(department);
			entityManager.persist(employee);
		});
	}

	@Test
	public void tesDerivedRoot(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			String query =
					" select emp_external.empId empId, dep_lateral.departmentId deptId from "
					+ " ( "
					+ "  select emp_internal.empId empId, emp_internal.department.id deptId from MyEmployee emp_internal "
					+ " ) emp_external "
					+ " join lateral "
					+ " ( "
					+ "  select department.id departmentId from MyDepartment department"
					+ "  where department.id = emp_external.deptId "
					+ "  and department.name = :name "
					+ " ) dep_lateral";

			List<IdsDTO> resultList = entityManager.createQuery(query, IdsDTO.class)
					.setParameter("name", "TEST DEPARTMENT")
					.getResultList();
			assertThat(resultList.size()).isEqualTo(1);

		});
	}


	@Test
	public void tesCte(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {


			String query=" with departements as ( "
					+ "           select department.id departmentId from MyDepartment department"
					+ "           where department.id=:idDept  ) "
					+ "       select cte.departmentId from departements cte"
					+ "       where exists ( "
					+ "           select 1 from MyEmployee emp "
					+ "           where emp.department.id = cte.departmentId "
					+ "       ) ";


			List<Long> resultList = entityManager.createQuery(query, Long.class)
					.setParameter("idDept", 1)
					.getResultList();
			assertThat(resultList.size()).isEqualTo(1);

		});
	}



	public static class IdsDTO {

		private Long empId;

		private Long deptId;

		public IdsDTO(Long empId, Long deptId) {
			this.empId = empId;
			this.deptId = deptId;
		}

		public Long getEmpId() {
			return empId;
		}

		public void setEmpId(Long empId) {
			this.empId = empId;
		}

		public Long getDeptId() {
			return deptId;
		}

		public void setDeptId(Long deptId) {
			this.deptId = deptId;
		}
	}


	@Entity(name="MyDepartment")
	@Table(name = "MY_DEPARTMENT")
	public class MyDepartment {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "DEPT_ID")
		private Long id;

		@Column(name = "DEPT_NAME", nullable = false, unique = true)
		private String name;

		public MyDepartment() {
		}

		public MyDepartment(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setName(String name) {
			this.name = name;
		}

		// equals & hashCode per l'identità

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof MyDepartment))
				return false;
			MyDepartment that = (MyDepartment) o;
			return Objects.equals(id, that.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id);
		}
	}

	@Entity(name="MyEmployee")
	@Table(name = "MY_EMPLOYEE")
	public class MyEmployee {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "EMP_ID")
		private Long empId;

		@Column(name = "FIRST_NAME")
		private String firstName;

		@Column(name = "LAST_NAME")
		private String lastName;

		@Column(name = "EMAIL", unique = true)
		private String email;


		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "DEPT_ID")
		private MyDepartment department;

		public MyDepartment getDepartment() {
			return department;
		}

		public void setDepartment(MyDepartment department) {
			this.department = department;
		}

		public MyEmployee() {
			super();
		}

		public MyEmployee(String firstName, String lastName, String email,MyDepartment department) {
			super();
			this.firstName = firstName;
			this.lastName = lastName;
			this.email = email;
			this.department=department;
		}

		public MyEmployee(Long empId, String firstName, String lastName, String email) {
			super();
			this.empId = empId;
			this.firstName = firstName;
			this.lastName = lastName;
			this.email = email;
		}

		public Long getEmpId() {
			return empId;
		}

		public void setEmpId(Long empId) {
			this.empId = empId;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof MyEmployee))
				return false;
			MyEmployee that = (MyEmployee) o;
			return Objects.equals(empId, that.empId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(empId);
		}

	}

}
