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
jmp _if_body_1
_if_body_1:
mov $1, %r12d
jmp _after_if_0
_else_body_1:
jmp _else_body_2
_if_body_2:
jmp _after_if_1
_else_body_2:
jmp _after_if_1
_after_if_1:
mov $0, %r12d
jmp _after_if_0
_after_if_0:
cmp $1, %r12d
je _if_body_0
jmp _else_body_0
_if_body_0:
mov $52, %r13d
jmp _after_if_2
_else_body_0:
mov $84, %r13d
jmp _after_if_2
_after_if_2:
mov %r13d, %r12d
jmp _basic_1
_basic_1:
mov %r12d, %eax
cltq
ret
