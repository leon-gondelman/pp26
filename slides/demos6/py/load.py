# Part A, elsewhere. The same load in Python, started in three ways:
#   python3 load.py bare | task | awaited
import asyncio, sys

async def fetch_user():
    await asyncio.sleep(0.3)
    return "Ada"

async def load():
    print("A")
    user = await fetch_user()
    print("C", user)

async def bare():
    load()                          # the call alone: it makes a value and runs nothing
    print("B")
    await asyncio.sleep(0.5)

async def task():
    asyncio.create_task(load())     # the work is put in the queue
    print("B")
    await asyncio.sleep(0.5)

async def awaited():
    await load()
    print("B")

asyncio.run({"bare": bare, "task": task, "awaited": awaited}[sys.argv[1]]())
