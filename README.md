# fulltextSearch


service mysqld start


mysql  설정파일 위치 
more /etc/my.cnf



[mysqld]
ft_min_word_len = 1
innodb_ft_min_token_size = 1


fulltext index  생성
alter table qa_index_tb  add FULLTEXT('question');


FULLTEXT INDEX `ft_question_idx` (`question`)


인덱스 재생성

SET GLOBAL innodb_optimize_fulltext_only=ON;

DROP INDEX ft_question_idx ON qa_index_tb;

CREATE FULLTEXT INDEX ft_question_idx ON qa_index_tb(question);
