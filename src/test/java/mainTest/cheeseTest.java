package mainTest;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class cheeseTest {
	/*@Autowired
	EntityManager em;

	@Test
	void contextLoads() {
		Users users = new Users();
		em.persist(users);

		JPAQueryFactory query = new JPAQueryFactory(em);
		QUsers qUsers = new QUsers("h");
		//		QHello qHello = QHello.hello;
		//    요런식으로 사용 가능

		Users result = query
			.selectFrom(qUsers)
			.fetchOne();

		Assertions.assertThat(result).isEqualTo(users);
	}*/
}
