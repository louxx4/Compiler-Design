int main() {
int a = 69;
int b = 420;
int t;

t = b;
b = a % b;
a = t;

t = b;
b = a % b;
a = t;

t = b;
b = a % b;
a = t;

t = b;
b = a % b;
a = t;

return a;
}