class Course:
    def __init__(self, type, subject, number, title, description, gened, requirements, notes):
        self.type = type
        self.subject = subject
        self.number = number
        self.title = title
        self.description = description
        self.gened = gened
        self.requirements = requirements
        self.notes = notes
    
    def to_dict(self):
        return self.__dict__

    def __str__(self):
        return f"{', '.join(f'{key}={value}' for key, value in self.__dict__.items())}"