.global main
.global _main
.text
main:
call _main
# move the return value into the first argument for the syscall
movq %rax, %rdi
# move the exit syscall number into rax
movq $0x3C, %rax
syscall
_main:
_basic_0:
mov $1, %r12b
cmp $1, %r12b
je _if_body_0
jmp _else_body_0
_else_body_0:
jmp _after_if_0
_if_body_0:
mov $-65531, %r13d
mov %r13d, %r12d
jmp _basic_1
_after_if_0:
mov $0, %r12d
jmp _basic_1
_basic_1:
mov %r12d, %eax
cltq
ret
