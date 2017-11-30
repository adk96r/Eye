
def foo(a,b,c):
    return sum(a,b,c)

l = [1,2,3,4]
print(foo(*l) + "in mul.")
