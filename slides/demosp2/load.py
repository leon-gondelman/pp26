import asyncio

async def fetch_user():
    await asyncio.sleep(0.3)
    return "Ada"

async def load():
    print("A")
    user = await fetch_user()
    print("C", user)

async def main():
    asyncio.create_task(load())
    print("B")
    await asyncio.sleep(1)

asyncio.run(main())
