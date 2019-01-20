
SBT = sbt

tests:
	$(SBT) "test:runMain dclib.Main"
