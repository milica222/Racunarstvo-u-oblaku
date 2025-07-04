from pydantic import BaseModel

class BookBase(BaseModel):
    title: str
    author: str
    description: str | None = None
    year: int

class BookCreate(BookBase):
    pass

class Book(BookBase):
    id: int

    class Config:
        orm_mode = True
