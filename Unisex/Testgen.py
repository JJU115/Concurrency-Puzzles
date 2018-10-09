import random

print(10)
d = {1:'M', 2:'F'}
for i in range(1,11):
	print(str(i) + "," + str(d[random.randint(1,2)]) + "," + str(random.randint(1,10)) + "," + str(random.randint(1,12)))
